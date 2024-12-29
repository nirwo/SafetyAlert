package nir.wolff.ui.alert

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import nir.wolff.service.AlertService

class SafetyAlertDialog : DialogFragment() {
    private val alertService = AlertService()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String): SafetyAlertDialog {
            return SafetyAlertDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val groupId = requireArguments().getString(ARG_GROUP_ID)
            ?: throw IllegalArgumentException("Group ID is required")

        return AlertDialog.Builder(requireContext())
            .setTitle("Safety Check")
            .setMessage("Are you safe? Please update your status.")
            .setPositiveButton("I'm Safe") { _, _ ->
                updateStatus(groupId, true)
            }
            .setNegativeButton("Need Help") { _, _ ->
                updateStatus(groupId, false)
            }
            .setCancelable(false)
            .create()
    }

    private fun updateStatus(groupId: String, isSafe: Boolean) {
        lifecycleScope.launch {
            try {
                auth.currentUser?.uid?.let { userId ->
                    alertService.markUserStatus(userId, groupId, isSafe)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
