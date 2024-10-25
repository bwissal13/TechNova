package com.techNova.controller;

import com.techNova.entity.User;
import com.techNova.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
@Setter
public class UserController implements Controller {

    private  UserService userService;
    private  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.endsWith("/users") && method.equals("GET")) {
            return listUsers();
        } else if (path.endsWith("/users/new") && method.equals("GET")) {
            return newUserForm();
        } else if (path.matches("/users/\\d+/edit") && method.equals("GET")) {
            Long id = extractIdFromPath(path);
            return editUserForm(id);
        } else if (path.matches("/users/\\d+/delete") && method.equals("GET")) {
            Long id = extractIdFromPath(path);
            return deleteUser(id);
        } else if (path.endsWith("/users/save") && method.equals("POST")) {
            return handleUserSave(request);
        }

        return new ModelAndView("redirect:/users");
    }

    private ModelAndView handleUserSave(HttpServletRequest request) {
        String idParam = request.getParameter("id");
        ModelAndView modelAndView;
        User user;

        try {
            String identification = request.getParameter("identification");
            Long userId = idParam != null && !idParam.isEmpty() ? Long.parseLong(idParam) : null;

            Optional<User> existingUser = userService.getUserByUsername(identification);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                modelAndView = new ModelAndView(userId != null ? "editUser" : "userForm");
                user = userId != null ? userService.findById(userId).orElse(new User()) : new User();
                modelAndView.addObject("user", user);
                modelAndView.addObject("error", "Identification document already exists");
                return modelAndView;
            }

            String expirationDateStr = request.getParameter("expirationDate");
            if (expirationDateStr != null && !expirationDateStr.isEmpty()) {
                Date expirationDate = dateFormat.parse(expirationDateStr);
                Date registrationDate = new Date();
                if (expirationDate.before(registrationDate)) {
                    modelAndView = new ModelAndView(userId != null ? "editUser" : "userForm");
                    user = userId != null ? userService.findById(userId).orElse(new User()) : new User();
                    updateUserFields(user, request);
                    modelAndView.addObject("user", user);
                    modelAndView.addObject("error", "Expiration date cannot be before registration date");
                    return modelAndView;
                }
            }

            if (userId != null) {
                Optional<User> optionalUser = userService.findById(userId);
                if (optionalUser.isPresent()) {
                    user = optionalUser.get();
                    updateUserFields(user, request);
                    userService.update(user);
                } else {
                    return new ModelAndView("redirect:/users");
                }
            } else {
                user = new User();
                updateUserFields(user, request);
                userService.create(user);
            }

            return new ModelAndView("redirect:/users");

        } catch (Exception e) {
            modelAndView = new ModelAndView(idParam != null ? "editUser" : "userForm");
            user = new User();
            updateUserFields(user, request);
            modelAndView.addObject("user", user);
            modelAndView.addObject("error", "An error occurred: " + e.getMessage());
            return modelAndView;
        }
    }

    private void updateUserFields(User user, HttpServletRequest request) {
        user.setUsername(request.getParameter("username"));
        user.setEmail(request.getParameter("email"));
        user.setFirstName(request.getParameter("firstName"));
        user.setLastName(request.getParameter("lastName"));
        user.setIdentification(request.getParameter("identification"));
        user.setNationality(request.getParameter("nationality"));

        try {
            String registrationDateStr = request.getParameter("registrationDate");
            if (registrationDateStr != null && !registrationDateStr.isEmpty()) {
                user.setRegistrationDate(dateFormat.parse(registrationDateStr));
            } else {
                user.setRegistrationDate(new Date());
            }

            String expirationDateStr = request.getParameter("expirationDate");
            if (expirationDateStr != null && !expirationDateStr.isEmpty()) {
                user.setExpirationDate(dateFormat.parse(expirationDateStr));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format", e);
        }
    }

    private ModelAndView listUsers() {
        ModelAndView modelAndView = new ModelAndView("userList");
        modelAndView.addObject("users", userService.findAll());
        return modelAndView;
    }

    private ModelAndView newUserForm() {
        ModelAndView modelAndView = new ModelAndView("userForm");
        User user = new User();
        user.setExpirationDate(new Date(System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000))); // 1 year from now
        modelAndView.addObject("user", user);
        modelAndView.addObject("minDate", new Date());
        return modelAndView;
    }

    private ModelAndView editUserForm(Long id) {
        ModelAndView modelAndView = new ModelAndView("editUser");
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            modelAndView.addObject("user", user.get());
            modelAndView.addObject("minDate", user.get().getRegistrationDate());
        } else {
            return new ModelAndView("redirect:/users");
        }
        return modelAndView;
    }

    private ModelAndView deleteUser(Long id) {
        userService.delete(id);
        return new ModelAndView("redirect:/users");
    }

    private Long extractIdFromPath(String path) {
        String[] segments = path.split("/");
        return Long.parseLong(segments[segments.length - 2]);
    }
}
