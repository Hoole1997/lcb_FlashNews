package com.example.lcb.app.news.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.example.lcb.app.BuildConfig
import com.example.lcb.app.R
import com.example.lcb.app.databinding.FragmentFlashMeBinding
import com.example.lcb.app.language.AppLanguageManager
import com.example.lcb.app.ui.FragmentSystemBars
import com.example.lcb.app.utils.NativeAdPosition
import com.example.lcb.app.utils.loadNative

class MeFragment : Fragment() {
    private var _binding: FragmentFlashMeBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFlashMeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configureSystemBars()
        configureLanguageRow()
        configureExternalRows()
        configureNativeAd()

        binding.privacyRow.settingIcon.setImageResource(R.drawable.ic_flash_privacy)
        binding.privacyRow.settingLabel.text = getString(R.string.flash_setting_privacy)

        binding.feedbackRow.settingIcon.setImageResource(R.drawable.ic_flash_feedback)
        binding.feedbackRow.settingLabel.text = getString(R.string.flash_setting_feedback)

        binding.aboutRow.settingIcon.setImageResource(R.drawable.ic_flash_about)
        binding.aboutRow.settingLabel.text = getString(R.string.flash_setting_version)
        binding.aboutRow.settingValue.text = BuildConfig.VERSION_NAME
        binding.aboutRow.settingChevron.visibility = View.GONE
    }

    private fun configureExternalRows() {
        binding.privacyRow.root.setOnClickListener {
            if (!MeExternalNavigator.openPrivacy(requireContext())) {
                showActionUnavailable(R.string.flash_privacy_open_failed)
            }
        }
        binding.feedbackRow.root.setOnClickListener {
            if (!MeExternalNavigator.openFeedback(requireContext())) {
                showActionUnavailable(R.string.flash_feedback_open_failed)
            }
        }
    }

    private fun showActionUnavailable(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun configureNativeAd() {
        activity?.loadNative(
            container = binding.meNativeAdContainer,
            position = NativeAdPosition.ME_SETTINGS,
        )
    }

    private fun configureLanguageRow() {
        childFragmentManager.setFragmentResultListener(
            LanguageBottomSheetDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, _ ->
            renderLanguageValue()
        }
        binding.languageRow.root.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(LanguageBottomSheetDialogFragment.TAG) == null) {
                LanguageBottomSheetDialogFragment().show(
                    childFragmentManager,
                    LanguageBottomSheetDialogFragment.TAG,
                )
            }
        }
        renderLanguageValue()
    }

    private fun renderLanguageValue() {
        binding.languageRow.settingValue.text =
            AppLanguageManager.selectedLanguage().label(requireContext())
    }

    private fun configureSystemBars() {
        FragmentSystemBars.applyEdgeToEdge(this)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            FragmentSystemBars.applyTopInsetToFixedHeightView(binding.settingTitle, insets)
            FragmentSystemBars.applyBottomNavigationInsets(requireActivity(), insets)
            insets
        }
        FragmentSystemBars.requestInsets(binding.root)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
