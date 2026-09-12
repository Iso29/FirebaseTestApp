package com.example.firebasetestapp.presentation.ui.login

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
import com.example.firebasetestapp.R
import com.example.firebasetestapp.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    private var isEmailMode = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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
        toggleLoginType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            isEmailMode = checkedId == R.id.btnTabEmail
            applyLoginType(requestFocus = true)
        }
        // The toggle restores its own checked state after rotation, so read it back here.
        isEmailMode = toggleLoginType.checkedButtonId != R.id.btnTabPhone
        applyLoginType(requestFocus = false)
    }

    private fun applyLoginType(requestFocus: Boolean) = with(binding) {
        tilEmail.isVisible = isEmailMode
        tilPhone.isVisible = !isEmailMode

        // Phone login is verified by SMS code, so there's no password to ask for.
        tilPassword.isVisible = isEmailMode
        btnForgotPassword.isVisible = isEmailMode
        btnLogin.setText(if (isEmailMode) R.string.login_action else R.string.login_action_send_code)

        viewModel.clearEmailError()
        viewModel.clearPhoneError()
        viewModel.clearPasswordError()

        if (requestFocus) {
            (if (isEmailMode) etEmail else etPhone).requestFocus()
        }
    }

    private fun setupInputs() = with(binding) {
        etEmail.doAfterTextChanged { viewModel.clearEmailError() }
        etPhone.doAfterTextChanged { viewModel.clearPhoneError() }
        etPassword.doAfterTextChanged { viewModel.clearPasswordError() }

        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else {
                false
            }
        }
    }

    private fun setupClicks() = with(binding) {
        btnLogin.setOnClickListener { login() }

        btnForgotPassword.setOnClickListener {
            viewModel.onForgotPasswordClicked(etEmail.text?.toString().orEmpty())
        }

        btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun login() = with(binding) {
        viewModel.login(
            identifier = (if (isEmailMode) etEmail.text else etPhone.text)?.toString().orEmpty(),
            password = etPassword.text?.toString().orEmpty(),
            isEmailMode = isEmailMode
        )
    }

    private fun observeViewModel() = with(binding) {
        viewModel.loading.observe(viewLifecycleOwner) { setLoading(it) }

        viewModel.emailError.observe(viewLifecycleOwner) { tilEmail.error = it.asText() }
        viewModel.phoneError.observe(viewLifecycleOwner) { tilPhone.error = it.asText() }
        viewModel.passwordError.observe(viewLifecycleOwner) { tilPassword.error = it.asText() }

        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { messageRes ->
                Snackbar.make(root, messageRes, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.loginSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }

        viewModel.navigateToOtp.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { phoneNumber ->
                findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToOtpFragment(
                        phoneNumber = phoneNumber,
                        name = null
                    )
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) = with(binding) {
        progressLogin.isVisible = isLoading
        btnLogin.text = when {
            isLoading -> ""
            isEmailMode -> getString(R.string.login_action)
            else -> getString(R.string.login_action_send_code)
        }
        btnLogin.isEnabled = !isLoading
        toggleLoginType.isEnabled = !isLoading
        tilEmail.isEnabled = !isLoading
        tilPhone.isEnabled = !isLoading
        tilPassword.isEnabled = !isLoading
        btnForgotPassword.isEnabled = !isLoading
        btnRegister.isEnabled = !isLoading
    }

    private fun Int?.asText(): String? = this?.let { getString(it) }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}