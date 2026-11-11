package com.asg.rocketflyiii.firefly;

/**
 * Supported Firefly III REST API endpoints.
 *
 * <p>Changes to these contracts require compatibility verification and a RocketFlyIII release.
 */
public final class FireflyEndpoints {

    private FireflyEndpoints() {
        // Prevent instantiation.
    }

    public static final String ABOUT = "/api/v1/about";
    public static final String ACCOUNTS = "/api/v1/accounts";
}
