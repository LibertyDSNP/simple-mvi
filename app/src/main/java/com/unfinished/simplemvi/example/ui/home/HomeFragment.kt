package com.unfinished.simplemvi.example.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.unfinished.simplemvi.example.databinding.FragmentHomeBinding
import com.unfinished.simplemvi.example.ui.postlist.PostListActivity

/**
 * A simple fragment that doesn't have any (significant) business logic. Because of that MVI is not used here.
 *
 */
class HomeFragment: Fragment() {
    lateinit var binding: FragmentHomeBinding

    companion object {
        fun newInstance() = HomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.searchView.searchResultSelectedListener = {
            startActivity(PostListActivity.createIntent(requireContext(), it))
        }
    }
}