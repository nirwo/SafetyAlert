// Generated by view binder compiler. Do not edit!
package nir.wolff.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import nir.wolff.R;

public final class ItemAlertBinding implements ViewBinding {
  @NonNull
  private final CardView rootView;

  @NonNull
  public final TextView alertAreaTextView;

  @NonNull
  public final TextView alertDescriptionTextView;

  @NonNull
  public final TextView alertTimeTextView;

  @NonNull
  public final TextView alertTitleTextView;

  private ItemAlertBinding(@NonNull CardView rootView, @NonNull TextView alertAreaTextView,
      @NonNull TextView alertDescriptionTextView, @NonNull TextView alertTimeTextView,
      @NonNull TextView alertTitleTextView) {
    this.rootView = rootView;
    this.alertAreaTextView = alertAreaTextView;
    this.alertDescriptionTextView = alertDescriptionTextView;
    this.alertTimeTextView = alertTimeTextView;
    this.alertTitleTextView = alertTitleTextView;
  }

  @Override
  @NonNull
  public CardView getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemAlertBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemAlertBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_alert, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemAlertBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.alertAreaTextView;
      TextView alertAreaTextView = ViewBindings.findChildViewById(rootView, id);
      if (alertAreaTextView == null) {
        break missingId;
      }

      id = R.id.alertDescriptionTextView;
      TextView alertDescriptionTextView = ViewBindings.findChildViewById(rootView, id);
      if (alertDescriptionTextView == null) {
        break missingId;
      }

      id = R.id.alertTimeTextView;
      TextView alertTimeTextView = ViewBindings.findChildViewById(rootView, id);
      if (alertTimeTextView == null) {
        break missingId;
      }

      id = R.id.alertTitleTextView;
      TextView alertTitleTextView = ViewBindings.findChildViewById(rootView, id);
      if (alertTitleTextView == null) {
        break missingId;
      }

      return new ItemAlertBinding((CardView) rootView, alertAreaTextView, alertDescriptionTextView,
          alertTimeTextView, alertTitleTextView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
