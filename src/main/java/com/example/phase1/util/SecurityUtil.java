package com.example.phase1.util;

import com.example.phase1.entity.User;

public class SecurityUtil {

    public static boolean isOrganization(User user) {
        return "organization".equals(user.getRole());
    }

    public static boolean isDonor(User user) {
        return "donor".equals(user.getRole());
    }
}