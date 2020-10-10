package com.github.toxrink.indextools.core.security;

import com.github.toxrink.indextools.core.model.UserPermission;
import scala.Option;

/**
 * Created by xw on 2019/12/24.
 */
@FunctionalInterface
public interface UserCheck {
    Option<UserPermission> checkUserPermission(String name, String passowrd);
}
