package org.odk.collect.android.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.R;
import org.odk.collect.android.support.RobolectricHelpers;
import org.odk.collect.android.support.TestActivityScenario;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDialog;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.odk.collect.android.preferences.AdminKeys.KEY_ADMIN_PW;
import static org.odk.collect.android.preferences.AdminPreferencesActivity.ADMIN_PREFERENCES;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ChangeAdminPasswordDialogTest {

    private FragmentManager fragmentManager;
    private ChangeAdminPasswordDialog dialogFragment;
    private SharedPreferences sharedPreferences;

    @Before
    public void setup() {
        FragmentActivity activity = RobolectricHelpers.createThemedActivity(FragmentActivity.class);
        sharedPreferences = activity.getSharedPreferences(ADMIN_PREFERENCES, Context.MODE_PRIVATE);

        fragmentManager = activity.getSupportFragmentManager();
        dialogFragment = new ChangeAdminPasswordDialog();
    }

    @Test
    public void dialogIsCancellable() {
        dialogFragment.show(fragmentManager, "TAG");
        assertThat(shadowOf(dialogFragment.getDialog()).isCancelable(), equalTo(true));
    }

    @Test
    public void clickingOkAfterSettingPassword_setsPasswordInSharedPreferences() {
        dialogFragment.show(fragmentManager, "TAG");
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        EditText passwordEditText = dialog.findViewById(R.id.pwd_field);
        passwordEditText.setText("blah");
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertThat(sharedPreferences.getString(KEY_ADMIN_PW, ""), equalTo("blah"));
    }

    @Test
    public void whenScreenIsRotated_passwordAndCheckboxValueIsRetained() {
        TestActivityScenario<DialogFragmentTestActivity> activityScenario = TestActivityScenario.launch(DialogFragmentTestActivity.class);
        activityScenario.onActivity(activity -> {
            dialogFragment.show(activity.getSupportFragmentManager(), "TAG");
            AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
            ((EditText) dialog.findViewById(R.id.pwd_field)).setText("blah");
            ((CheckBox) dialog.findViewById(R.id.checkBox2)).setChecked(true);
        });

        activityScenario.recreate();

        activityScenario.onActivity(activity -> {
            ChangeAdminPasswordDialog restoredFragment = (ChangeAdminPasswordDialog) activity.getSupportFragmentManager().findFragmentByTag("TAG");
            AlertDialog restoredDialog = (AlertDialog) restoredFragment.getDialog();
            assertThat(((EditText) restoredDialog.findViewById(R.id.pwd_field)).getText().toString(), equalTo("blah"));
            assertThat(((CheckBox) restoredDialog.findViewById(R.id.checkBox2)).isChecked(), equalTo(true));
        });
    }

    @Test
    public void clickingOk_dismissesTheDialog() {
        dialogFragment.show(fragmentManager, "TAG");
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertTrue(dialog.isShowing());

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertFalse(dialog.isShowing());
        assertTrue(shadowOf(dialog).hasBeenDismissed());
    }

    @Test
    public void clickingCancel_dismissesTheDialog() {
        dialogFragment.show(fragmentManager, "TAG");
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertTrue(dialog.isShowing());

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        assertFalse(dialog.isShowing());
        assertTrue(shadowOf(dialog).hasBeenDismissed());
    }

    @Test
    public void checkingShowPassword_displaysPasswordAsText() {
        dialogFragment.show(fragmentManager, "TAG");
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();

        EditText passwordEditText = dialog.findViewById(R.id.pwd_field);
        CheckBox passwordCheckBox = dialog.findViewById(R.id.checkBox2);

        passwordCheckBox.setChecked(true);
        assertThat(passwordEditText.getInputType(), equalTo(InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    @Test
    public void uncheckingShowPassword_displaysPasswordAsPassword() {
        dialogFragment.show(fragmentManager, "TAG");
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();

        EditText passwordEditText = dialog.findViewById(R.id.pwd_field);
        CheckBox passwordCheckBox = dialog.findViewById(R.id.checkBox2);

        passwordCheckBox.setChecked(false);
        assertThat(passwordEditText.getInputType(), equalTo(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    private static class DialogFragmentTestActivity extends FragmentActivity {

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(R.style.Theme_AppCompat); // Needed for androidx.appcompat.app.AlertDialog
        }
    }
}
