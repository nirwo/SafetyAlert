package nir.wolff.ui.alerts

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class AlertsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AlertsCurrentFragment.newInstance()
            1 -> AlertHistoryFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}
