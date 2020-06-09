package org.odk.collect.android.support.pages;

import androidx.test.rule.ActivityTestRule;

import org.odk.collect.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class FormMetadataPage extends PreferencePage<FormMetadataPage> {

    public FormMetadataPage(ActivityTestRule rule) {
        super(rule);
    }

    @Override
    public FormMetadataPage assertOnPage() {
        checkIsStringDisplayed(R.string.form_metadata_title);
        return this;
    }

    public FormMetadataPage clickEmail() {
        onView(withText(getTranslatedString(R.string.email))).perform(click());
        return this;
    }

    public FormMetadataPage clickUsername() {
        onView(withText(getTranslatedString(R.string.username))).perform(click());
        return this;
    }

    public FormMetadataPage clickPhoneNumber() {
        onView(withText(getTranslatedString(R.string.phone_number))).perform(click());
        return this;
    }
}
