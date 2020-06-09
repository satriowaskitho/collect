package org.odk.collect.android.support.pages;

import androidx.test.rule.ActivityTestRule;

import org.odk.collect.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class GetBlankFormPage extends Page<GetBlankFormPage> {

    GetBlankFormPage(ActivityTestRule rule) {
        super(rule);
    }

    @Override
    public GetBlankFormPage assertOnPage() {
        onView(withText(getTranslatedString(R.string.get_forms))).check(matches(isDisplayed()));
        return this;
    }
}
