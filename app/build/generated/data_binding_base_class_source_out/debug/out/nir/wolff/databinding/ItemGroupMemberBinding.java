// Generated by view binder compiler. Do not edit!
package nir.wolff.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.google.android.material.card.MaterialCardView;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import nir.wolff.R;

public final class ItemGroupMemberBinding implements ViewBinding {
  @NonNull
  private final MaterialCardView rootView;

  @NonNull
  public final TextView memberEmail;

  @NonNull
  public final TextView memberStatus;

  @NonNull
  public final ImageButton removeMemberButton;

  private ItemGroupMemberBinding(@NonNull MaterialCardView rootView, @NonNull TextView memberEmail,
      @NonNull TextView memberStatus, @NonNull ImageButton removeMemberButton) {
    this.rootView = rootView;
    this.memberEmail = memberEmail;
    this.memberStatus = memberStatus;
    this.removeMemberButton = removeMemberButton;
  }

  @Override
  @NonNull
  public MaterialCardView getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemGroupMemberBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemGroupMemberBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_group_member, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemGroupMemberBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.memberEmail;
      TextView memberEmail = ViewBindings.findChildViewById(rootView, id);
      if (memberEmail == null) {
        break missingId;
      }

      id = R.id.memberStatus;
      TextView memberStatus = ViewBindings.findChildViewById(rootView, id);
      if (memberStatus == null) {
        break missingId;
      }

      id = R.id.removeMemberButton;
      ImageButton removeMemberButton = ViewBindings.findChildViewById(rootView, id);
      if (removeMemberButton == null) {
        break missingId;
      }

      return new ItemGroupMemberBinding((MaterialCardView) rootView, memberEmail, memberStatus,
          removeMemberButton);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
