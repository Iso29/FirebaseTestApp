package com.example.firebasetestapp.presentation.ui.verification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.firebasetestapp.R
import com.example.firebasetestapp.databinding.FragmentOtpBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val args: OtpFragmentArgs by navArgs()
    private val viewModel: OtpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSubtitle.text = getString(R.string.otp_subtitle, "args.phoneNumber")

        setupInputs()
        setupClicks()
        observeViewModel()

        // No-ops if the code was already sent — the ViewModel survives rotation.
        viewModel.start(requireActivity(), args.phoneNumber,args.name)
    }

    private fun setupInputs() = with(binding) {
        etOtp.doAfterTextChanged { text ->
            viewModel.clearCodeError()
            // Submit automatically once all six digits are in.
            if (text?.length == CODE_LENGTH) viewModel.onVerifyClicked(text.toString())
        }

        etOtp.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.onVerifyClicked(etOtp.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun setupClicks() = with(binding) {
        btnVerify.setOnClickListener {
            viewModel.onVerifyClicked(etOtp.text?.toString().orEmpty())
        }
        btnResend.setOnClickListener { viewModel.onResendClicked(requireActivity()) }
        btnChangeNumber.setOnClickListener { findNavController().popBackStack() }
    }

    private fun observeViewModel() = with(binding) {
        viewModel.loading.observe(viewLifecycleOwner) { setLoading(it) }

        viewModel.codeError.observe(viewLifecycleOwner) { tilOtp.error = it?.let { res -> getString(res) } }

        viewModel.secondsUntilResend.observe(viewLifecycleOwner) { seconds ->
            btnResend.isEnabled = seconds == 0 && viewModel.loading.value != true
            btnResend.text = if (seconds == 0) {
                getString(R.string.otp_action_resend)
            } else {
                getString(R.string.otp_action_resend_in, seconds / 60, seconds % 60)
            }
        }

        viewModel.autoFilledCode.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { code -> etOtp.setText(code) }
        }

        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { messageRes ->
                Snackbar.make(root, messageRes, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.verificationSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                // The user is signed in here. Point this at your home destination and clear
                // the auth screens off the back stack.
                findNavController().navigate(OtpFragmentDirections.actionOtpFragmentToHomeFragment())
            }
        }
    }

    private fun setLoading(isLoading: Boolean) = with(binding) {
        progressOtp.isVisible = isLoading
        btnVerify.text = if (isLoading) "" else getString(R.string.otp_action_verify)
        btnVerify.isEnabled = !isLoading
        tilOtp.isEnabled = !isLoading
        btnResend.isEnabled = !isLoading && viewModel.secondsUntilResend.value == 0
        btnChangeNumber.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val CODE_LENGTH = 6
    }
}