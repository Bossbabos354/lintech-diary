package ru.school28.diary.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.school28.diary.DairyFragment
import ru.school28.diary.MarksFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    val fragments = listOf(
        DairyFragment(),
        MarksFragment(),
        SettingsFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}