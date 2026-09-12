package com.example.firebasetestapp.presentation.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.firebasetestapp.R
import com.example.firebasetestapp.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    private var isEmailMode = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToggle()
        setupInputs()
        setupClicks()
        observeViewModel()
    }

    private fun setupToggle() = with(binding) {
        toggleRegisterType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            isEmailMode = checkedId == R.id.btnTabEmail
            applyLoginType(requestFocus = true)
        }
        // The toggle restores its own checked state after rotation, so read it back here.
        isEmailMode = toggleRegisterType.checkedButtonId != R.id.btnTabPhone
        applyLoginType(requestFocus = false)
    }

    private fun applyLoginType(requestFocus: Boolean) = with(binding) {
        tilEmail.isVisible = isEmailMode
        tilPhone.isVisible = !isEmailMode

        viewModel.clearEmailError()
        viewModel.clearPhoneError()

        if (requestFocus) {
            (if (isEmailMode) etEmail else etPhone).requestFocus()
        }
    }

    private fun setupInputs() = with(binding) {
        etName.doAfterTextChanged { viewModel.clearNameError() }
        etEmail.doAfterTextChanged { viewModel.clearEmailError() }
        etPhone.doAfterTextChanged { viewModel.clearPhoneError() }
        etPassword.doAfterTextChanged { viewModel.clearPasswordError() }
        etConfirmPassword.doAfterTextChanged { viewModel.clearConfirmPasswordError() }

        etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                register()
                true
            } else {
                false
            }
        }
    }

    private fun setupClicks() = with(binding) {
        btnRegister.setOnClickListener { register() }
        // Register is opened from login, so just go back instead of stacking screens.
        btnLogin.setOnClickListener { findNavController().popBackStack() }
    }

    private fun register() = with(binding) {
        viewModel.register(
            name = etName.text?.toString().orEmpty(),
            identifier = (if (isEmailMode) etEmail.text else etPhone.text)?.toString().orEmpty(),
            password = etPassword.text?.toString().orEmpty(),
            confirmPassword = etConfirmPassword.text?.toString().orEmpty(),
            termsAccepted = cbTerms.isChecked,
            isEmailMode = isEmailMode
        )
    }

    private fun observeViewModel() = with(binding) {
        viewModel.loading.observe(viewLifecycleOwner) { setLoading(it) }

        viewModel.nameError.observe(viewLifecycleOwner) { tilName.error = it.asText() }
        viewModel.emailError.observe(viewLifecycleOwner) { tilEmail.error = it.asText() }
        viewModel.phoneError.observe(viewLifecycleOwner) { tilPhone.error = it.asText() }
        viewModel.passwordError.observe(viewLifecycleOwner) { tilPassword.error = it.asText() }
        viewModel.confirmPasswordError.observe(viewLifecycleOwner) {
            tilConfirmPassword.error = it.asText()
        }

        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { messageRes ->
                Snackbar.make(root, messageRes, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.registerSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                // The user is already signed in here — navigate to your home destination
                // instead if you don't want to send them back to login.

                Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_LONG).show()
                findNavController().navigate(
                    RegisterFragmentDirections.actionRegisterFragmentToHomeFragment()
                )
            }
        }

        viewModel.navigateToOtp.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { phone ->
                findNavController().navigate(
                    RegisterFragmentDirections.actionRegisterFragmentToOtpFragment(
                        phoneNumber = phone,
                        name = binding.etName.text?.toString()
                    )
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) = with(binding) {
        progressRegister.isVisible = isLoading
        btnRegister.text = if (isLoading) "" else getString(R.string.register_action)
        btnRegister.isEnabled = !isLoading
        toggleRegisterType.isEnabled = !isLoading
        tilName.isEnabled = !isLoading
        tilEmail.isEnabled = !isLoading
        tilPhone.isEnabled = !isLoading
        tilPassword.isEnabled = !isLoading
        tilConfirmPassword.isEnabled = !isLoading
        cbTerms.isEnabled = !isLoading
    }

    private fun Int?.asText(): String? = this?.let { getString(it) }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}