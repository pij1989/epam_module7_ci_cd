package com.epam.esm.controller;

import com.epam.esm.model.entity.Order;
import com.epam.esm.model.entity.Tag;
import com.epam.esm.model.entity.User;
import com.epam.esm.model.exception.NotFoundException;
import com.epam.esm.model.hateoas.assembler.OrderModelAssembler;
import com.epam.esm.model.hateoas.assembler.TagModelAssembler;
import com.epam.esm.model.hateoas.assembler.UserModelAssembler;
import com.epam.esm.model.hateoas.model.OrderModel;
import com.epam.esm.model.hateoas.model.TagModel;
import com.epam.esm.model.hateoas.model.UserModel;
import com.epam.esm.model.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.epam.esm.model.error.MessageKeyError.*;

@RestController
@RequestMapping(value = "/users", produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserModelAssembler userModelAssembler;
    private final OrderModelAssembler orderModelAssembler;
    private final TagModelAssembler tagModelAssembler;

    @Autowired
    public UserController(UserService userService, UserModelAssembler userModelAssembler,
                          OrderModelAssembler orderModelAssembler, TagModelAssembler tagModelAssembler) {
        this.userService = userService;
        this.userModelAssembler = userModelAssembler;
        this.orderModelAssembler = orderModelAssembler;
        this.tagModelAssembler = tagModelAssembler;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<UserModel> createUser(@RequestBody User user) {
        logger.debug("USER: " + user);
        Optional<User> optionalUser = userService.createUser(user);
        if (optionalUser.isPresent()) {
            User createdUser = optionalUser.get();
            UserModel userModel = userModelAssembler.toModel(createdUser);
            return new ResponseEntity<>(userModel, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/{userId}/orders")
    public ResponseEntity<OrderModel> createUserOrder(@PathVariable Long userId, @RequestBody Order order) throws NotFoundException {
        logger.debug("USER'S ORDER:" + order);
        Optional<Order> optionalOrder = userService.createOrderForUser(userId, order);
        return optionalOrder.map(orderModelAssembler::toModel)
                .map(orderModel -> new ResponseEntity<>(orderModel, HttpStatus.CREATED))
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND, new Object[]{userId}));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserModel> findUser(@PathVariable Long id) throws NotFoundException {
        Optional<User> optionalUser = userService.findUser(id);
        return optionalUser.map(userModelAssembler::toModel)
                .map(userModel -> new ResponseEntity<>(userModel, HttpStatus.OK))
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND, new Object[]{id}));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<PagedModel<UserModel>> findUsers(@RequestParam(name = "page", defaultValue = RequestParameter.DEFAULT_PAGE_NUMBER) Integer page,
                                                           @RequestParam(name = "size", defaultValue = RequestParameter.DEFAULT_PAGE_SIZE) Integer size,
                                                           PagedResourcesAssembler<User> pagedResourcesAssembler) throws NotFoundException {
        Page<User> userPage = userService.findUsers(page, size);
        if (!userPage.isEmpty()) {
            PagedModel<UserModel> userModels = pagedResourcesAssembler.toModel(userPage, userModelAssembler);
            return new ResponseEntity<>(userModels, HttpStatus.OK);
        } else {
            throw new NotFoundException(USERS_NOT_FOUND, new Object[]{});
        }
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{userId}/orders")
    public ResponseEntity<PagedModel<OrderModel>> findOrdersForUser(@PathVariable Long userId,
                                                                    @RequestParam(name = "page", defaultValue = RequestParameter.DEFAULT_PAGE_NUMBER) Integer page,
                                                                    @RequestParam(name = "size", defaultValue = RequestParameter.DEFAULT_PAGE_SIZE) Integer size,
                                                                    PagedResourcesAssembler<Order> pagedResourcesAssembler) throws NotFoundException {
        Page<Order> orderPage = userService.findOrdersForUser(userId, page, size);
        if (orderPage == null) {
            throw new NotFoundException(USER_NOT_FOUND, new Object[]{userId});
        }
        if (!orderPage.isEmpty()) {
            PagedModel<OrderModel> orderModels = pagedResourcesAssembler.toModel(orderPage, orderModelAssembler);
            return new ResponseEntity<>(orderModels, HttpStatus.OK);
        } else {
            throw new NotFoundException(USER_ORDERS_NOT_FOUND, new Object[]{userId});
        }
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{userId}/orders/{orderId}")
    public ResponseEntity<OrderModel> findOrderForUser(@PathVariable Long userId,
                                                       @PathVariable Long orderId) throws NotFoundException {
        Optional<Order> optionalOrder = userService.findOrderForUser(userId, orderId);
        return optionalOrder.map(orderModelAssembler::toModel)
                .map(orderModel -> new ResponseEntity<>(orderModel, HttpStatus.OK))
                .orElseThrow(() -> new NotFoundException(USER_OR_ORDER_NOT_FOUND, new Object[]{userId, orderId}));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{userId}/orders/{orderId}")
    public ResponseEntity<UserModel> addOrderToUser(@PathVariable Long userId,
                                                    @PathVariable Long orderId) throws NotFoundException {
        Optional<User> optionalUser = userService.addOrderToUser(userId, orderId);
        return optionalUser.map(userModelAssembler::toModel)
                .map(userModel -> new ResponseEntity<>(userModel, HttpStatus.OK))
                .orElseThrow(() -> new NotFoundException(USER_OR_ORDER_NOT_FOUND, new Object[]{userId, orderId}));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/widely_used_tag")
    public ResponseEntity<TagModel> findWidelyUsedTagForUserWithHighestCostOfAllOrders() throws NotFoundException {
        Optional<Tag> optionalTag = userService.findWidelyUsedTagForUserWithHighestCostOfAllOrders();
        return optionalTag.map(tagModelAssembler::toModel)
                .map(tagModel -> new ResponseEntity<>(tagModel, HttpStatus.OK))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND, new Object[]{}));
    }
}
