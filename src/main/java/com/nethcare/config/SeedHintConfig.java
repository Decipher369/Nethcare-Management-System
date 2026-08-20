package com.nethcare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * login.html asks for this bean (@seedShowHint) to decide whether to print the
 * dev passwords. It only exists under the dev profile, so the hint cannot show
 * up anywhere else.
 */
@Component("seedShowHint")
@Profile("dev")
public class SeedHintConfig {

    private final boolean showHint;

    public SeedHintConfig(@Value("${seed.show-hint:false}") boolean showHint) {
        this.showHint = showHint;
    }

    public boolean isShowHint() {
        return showHint;
    }
}
