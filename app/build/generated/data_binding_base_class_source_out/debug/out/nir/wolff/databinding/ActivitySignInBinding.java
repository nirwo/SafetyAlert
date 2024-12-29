// Generated by view binder compiler. Do not edit!
package nir.wolff.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.google.android.gms.common.SignInButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import nir.wolff.R;

public final class ActivitySignInBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final ImageView appLogo;

  @NonNull
  public final TextView descriptionText;

  @NonNull
  public final ProgressBar progressBar;

  @NonNull
  public final SignInButton signInButton;

  @NonNull
  public final TextView welcomeText;

  private ActivitySignInBinding(@NonNull ConstraintLayout rootView, @NonNull ImageView appLogo,
      @NonNull TextView descriptionText, @NonNull ProgressBar progressBar,
      @NonNull SignInButton signInButton, @NonNull TextView welcomeText) {
    this.rootView = rootView;
    this.appLogo = appLogo;
    this.descriptionText = descriptionText;
    this.progressBar = progressBar;
    this.signInButton = signInButton;
    this.welcomeText = welcomeText;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivitySignInBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivitySignInBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_sign_in, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivitySignInBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.appLogo;
      ImageView appLogo = ViewBindings.findChildViewById(rootView, id);
      if (appLogo == null) {
        break missingId;
      }

      id = R.id.descriptionText;
      TextView descriptionText = ViewBindings.findChildViewById(rootView, id);
      if (descriptionText == null) {
        break missingId;
      }

      id = R.id.progressBar;
      ProgressBar progressBar = ViewBindings.findChildViewById(rootView, id);
      if (progressBar == null) {
        break missingId;
      }

      id = R.id.signInButton;
      SignInButton signInButton = ViewBindings.findChildViewById(rootView, id);
      if (signInButton == null) {
        break missingId;
      }

      id = R.id.welcomeText;
      TextView welcomeText = ViewBindings.findChildViewById(rootView, id);
      if (welcomeText == null) {
        break missingId;
      }

      return new ActivitySignInBinding((ConstraintLayout) rootView, appLogo, descriptionText,
          progressBar, signInButton, welcomeText);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
