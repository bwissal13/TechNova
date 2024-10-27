
package com.techNova.controller;

import com.techNova.entity.User;
import com.techNova.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
@Setter
public class UserController implements Controller {

    private  UserService userService;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.endsWith("/users") && method.equals("GET")) {
            return listUsers();
        } else if (path.endsWith("/users/new") && method.equals("GET")) {
            return newUserForm();
        } else if (path.matches(".*/users/\\d+/edit$") && method.equals("GET")) {
            Long id = extractIdFromPath(path);
            return editUserForm(id);
        } else if (path.matches(".*/users/\\d+/delete$") && method.equals("GET")) {
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
            // Get identification and check if it exists
            String identification = request.getParameter("identification");
            Long userId = idParam != null && !idParam.isEmpty() ? Long.parseLong(idParam) : null;

            if (userService.getUserByUsername(identification).isPresent() && (userId == null || !userService.findById(userId).isPresent())) {
                modelAndView = new ModelAndView(userId != null ? "editUser" : "userForm");
                user = userId != null ? userService.findById(userId).orElse(new User()) : new User();
                modelAndView.addObject("user", user);
                modelAndView.addObject("error", "Identification document already exists");
                return modelAndView;
            }

            // Validate expiration date
            String expirationDateStr = request.getParameter("expirationDate");
            if (expirationDateStr != null && !expirationDateStr.isEmpty()) {
                LocalDate expirationDate = LocalDate.parse(expirationDateStr);
                LocalDate registrationDate = LocalDate.now();
                if (expirationDate.isBefore(registrationDate)) {
                    modelAndView = new ModelAndView(userId != null ? "editUser" : "userForm");
                    user = userId != null ? userService.findById(userId).orElse(new User()) : new User();
                    updateUserFields(user, request);
                    modelAndView.addObject("user", user);
                    modelAndView.addObject("error", "Expiration date cannot be before registration date");
                    return modelAndView;
                }
            }

            // Process the save/update
            if (userId != null) {
                Optional<User> optionalUser = userService.findById(userId);
                if (optionalUser.isPresent()) {
                    user = optionalUser.get();
                    updateUserFields(user, request);
                } else {
                    return new ModelAndView("redirect:/users");
                }
            } else {
                user = new User();
                updateUserFields(user, request);
            }

            userService.create(user);
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
        user.setLastName(request.getParameter("lastName"));
        user.setFirstName(request.getParameter("firstName"));
        user.setIdentification(request.getParameter("identification"));
        user.setNationality(request.getParameter("nationality"));
        user.setUsername(request.getParameter("username"));
        user.setEmail(request.getParameter("email"));

        String expirationDateStr = request.getParameter("expirationDate");
        if (expirationDateStr != null && !expirationDateStr.isEmpty()) {
            try {
                LocalDate expirationDate = LocalDate.parse(expirationDateStr);
                user.setExpirationDate(Date.from(expirationDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format");
            }
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
        // Set default expiration date to one year from now
        user.setExpirationDate(Date.from(LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        modelAndView.addObject("user", user);
        modelAndView.addObject("minDate", Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
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
        return Long.parseLong(segments[segments.length - 2]); // ID should be second-to-last segment
    }

}
