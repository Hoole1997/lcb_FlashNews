package com.example.lcb.app.news.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lcb.app.databinding.DialogFlashLanguageBinding
import com.example.lcb.app.language.AppLanguageManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LanguageBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogFlashLanguageBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext()).apply {
            setOnShowListener { dialog ->
                val bottomSheet = (dialog as BottomSheetDialog)
                    .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogFlashLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = LanguageOptionAdapter { language ->
            // 先提交语言，再通知页面更新，避免结果监听器读取到旧 Locale。
            AppLanguageManager.selectLanguage(language)
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply { putString(KEY_LANGUAGE, language.key) },
            )
            dismissAllowingStateLoss()
        }
        binding.languageRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.languageRecycler.adapter = adapter
        adapter.submitSelectedKey(AppLanguageManager.selectedLanguage().key)
    }

    override fun onDestroyView() {
        binding.languageRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "language_bottom_sheet_result"
        const val KEY_LANGUAGE = "language_key"
        const val TAG = "LanguageBottomSheetDialogFragment"
    }
}
