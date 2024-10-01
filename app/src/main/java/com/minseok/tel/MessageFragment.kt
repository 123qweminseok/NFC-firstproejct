package com.minseok.tel

import MessageBusinessTrip
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.minseok.tel.databinding.FragmentMessageBinding

class MessageFragment : Fragment() {
    // 뷰 바인딩을 위한 변수
    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    // 프래그먼트에 전달된 인자를 저장하는 변수
    private var phoneNumber: String? = null
    private var name: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 프래그먼트에 전달된 인자를 받아옴
        arguments?.let {
            phoneNumber = it.getString(ARG_PHONE_NUMBER)
            name = it.getString(ARG_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 초기화
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLateApplication.setOnClickListener {
            val lateApplicationFragment = Messageperception()
            replaceFragment(lateApplicationFragment)
            animateButton(it)
        }
        binding.btnVacationRequest.setOnClickListener {
            val vacationRequestFragment = Messagevacation()
            replaceFragment(vacationRequestFragment)
            animateButton(it)
        }
        binding.btnBusinessTripManagement.setOnClickListener {
            val businessTripFragment = MessageBusinessTrip().apply {
                arguments = Bundle().apply {
                    putString("phoneNumber", phoneNumber)
                }
            }
            replaceFragment(businessTripFragment)
            animateButton(it)
                }
    }

    // 버튼 클릭 시 애니메이션 적용
    private fun animateButton(view: View) {
        val slideAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
        view.startAnimation(slideAnimation)
    }

    // 프래그먼트 교체 함수
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,  // 새 프래그먼트 진입 애니메이션
                R.anim.slide_out_left,  // 현재 프래그먼트 퇴장 애니메이션
                R.anim.slide_in_left,   // 이전 프래그먼트로 돌아올 때 진입 애니메이션
                R.anim.slide_out_right  // 이전 프래그먼트로 돌아갈 때 현재 프래그먼트 퇴장 애니메이션
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    // 뷰가 파괴될 때 바인딩 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // 프래그먼트 인자의 키 값
        private const val ARG_PHONE_NUMBER = "phone_number"
        private const val ARG_NAME = "name"

        // 프래그먼트 인스턴스 생성 함수
        @JvmStatic
        fun newInstance(phoneNumber: String, name: String) =
            MessageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHONE_NUMBER, phoneNumber)
                    putString(ARG_NAME, name)
                }
            }
    }
}