package com.minseok.tel

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.OnRangeSelectedListener
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import javax.crypto.spec.SecretKeySpec

class Messagevacation : Fragment(), OnDateSelectedListener, OnRangeSelectedListener {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var selectedDatesText: TextView
    private lateinit var submitButton: Button
    private lateinit var selectedDateDecorator: SelectedDateDecorator
    private var isAdmin: Boolean = false
    private lateinit var phoneNumber: String
    private lateinit var secretKey: SecretKeySpec
    private lateinit var database: DatabaseReference
    private var selectedDates: MutableSet<CalendarDay> = mutableSetOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃 인플레이트
        val view = inflater.inflate(R.layout.fragment_messagevacation, container, false)

        // 뷰 초기화
        calendarView = view.findViewById(R.id.calendarView)
        selectedDatesText = view.findViewById(R.id.selectedDatesText)
        submitButton = view.findViewById(R.id.submitButton)

        // 전화번호와 키 로드
        phoneNumber = arguments?.getString("phoneNumber") ?: ""
        secretKey = loadKey()
        database = FirebaseDatabase.getInstance().reference

        // 사용자 권한 확인
        checkUserPermission { isAdmin ->
            this.isAdmin = isAdmin
            activity?.runOnUiThread {
                if (isAdmin) {
                    setupUserView()
                } else {
                    setupAdminView()
                }
            }
        }

        // 캘린더뷰 설정
        calendarView.setOnDateChangedListener(this)
        calendarView.setOnRangeSelectedListener(this)
        calendarView.selectionMode = MaterialCalendarView.SELECTION_MODE_MULTIPLE

        // 데코레이터 추가
        selectedDateDecorator = SelectedDateDecorator(requireContext())
        calendarView.addDecorator(selectedDateDecorator)
        calendarView.addDecorator(TodayDecorator())

        // 버튼 클릭 리스너 설정
        submitButton.setOnClickListener {
            // 2. 이름을 입력받지 않고 선택된 날짜가 있을 때만 저장
            if (selectedDates.isNotEmpty()) {
                saveVacationToFirebase(selectedDates.toList())
            } else {
                Toast.makeText(context, "날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // 고정 키 로드 (ManagerActivity와 동일한 키)
    private fun loadKey(): SecretKeySpec {
        val fixedKeyString = "12345678901234567890123456789012"
        val fixedKey = fixedKeyString.toByteArray(Charsets.UTF_8)
        return SecretKeySpec(fixedKey, "AES")
    }

    // 사용자 권한 확인 함수
    private fun checkUserPermission(callback: (Boolean) -> Unit) {
        val encryptedPhoneNumber = EncryptionUtil.encrypt(phoneNumber, secretKey)

        database.orderByChild("value").equalTo(encryptedPhoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val encryptedPermission = userSnapshot.child("permission")
                                .getValue(String::class.java)
                            val decryptedPermission = EncryptionUtil.decrypt(
                                encryptedPermission ?: "", secretKey
                            )
                            callback(decryptedPermission == "admin")
                            return
                        }
                    }
                    callback(false) // 사용자를 찾지 못한 경우
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(
                        "Messagevacation",
                        "Error checking user permission",
                        databaseError.toException()
                    )
                    callback(false) // 오류 발생 시 기본값은 false (user)
                }
            })
    }

    // 관리자용 뷰 설정
    private fun setupAdminView() {
        submitButton.visibility = View.VISIBLE
    }

    // 사용자용 뷰 설정
    private fun setupUserView() {
        submitButton.visibility = View.GONE
    }

    // 휴가 정보를 Firebase에 저장
    private fun saveVacationToFirebase(dates: List<CalendarDay>) {
        val database = FirebaseDatabase.getInstance()
        val vacationsRef = database.getReference("vacations")

        dates.forEach { date ->
            val dateString = "${date.year}-${date.month}-${date.day}"
            // 이름을 저장하지 않음 (제거됨)
            vacationsRef.child(dateString).push().setValue("휴가")
                .addOnSuccessListener {
                    Toast.makeText(context, "휴가 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    selectedDates.clear()
                    calendarView.clearSelection()
                    updateSelectedDatesText()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "저장에 실패했습니다: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 날짜 선택 시 호출되는 메소드
    override fun onDateSelected(
        widget: MaterialCalendarView,
        date: CalendarDay,
        selected: Boolean
    ) {
        if (selected) {
            selectedDates.add(date)
            selectedDateDecorator.addDate(date)
        } else {
            selectedDates.remove(date)
            selectedDateDecorator.removeDate(date)
        }
        if (isAdmin) {
            updateSelectedDatesText()
        } else {
            loadVacationsForDate(date)
        }
        calendarView.invalidateDecorators()
    }

    // 범위 선택 시 호출되는 메소드
    override fun onRangeSelected(widget: MaterialCalendarView, dates: List<CalendarDay>) {
        selectedDates.addAll(dates)
        dates.forEach { selectedDateDecorator.addDate(it) }
        updateSelectedDatesText()
        calendarView.invalidateDecorators()
    }

    // 특정 날짜의 휴가 정보를 로드
    private fun loadVacationsForDate(date: CalendarDay) {
        val dateString = "${date.year}-${date.month}-${date.day}"
        val database = FirebaseDatabase.getInstance()
        val vacationsRef = database.getReference("vacations").child(dateString)

        vacationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val names = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                if (names.isNotEmpty()) {
                    selectedDatesText.text = "휴가자: ${names.joinToString(", ")}"
                } else {
                    selectedDatesText.text = "해당 날짜에 휴가자가 없습니다."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                selectedDatesText.text = "데이터 로딩 중 오류가 발생했습니다."
            }
        })
    }

    // 선택된 날짜 텍스트 업데이트
    private fun updateSelectedDatesText() {
        val dateStrings = selectedDates.map { "${it.year}년 ${it.month}월 ${it.day}일" }
        selectedDatesText.text = "선택된 날짜: ${dateStrings.joinToString(", ")}"
    }

    // 오늘 날짜를 강조 표시하는 데코레이터
    inner class TodayDecorator : DayViewDecorator {
        private val today = CalendarDay.today()

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return day == today
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(
                DotSpan(
                    5f,
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                )
            )
        }
    }

    // 선택된 날짜를 강조 표시하는 데코레이터
    inner class SelectedDateDecorator(context: android.content.Context) : DayViewDecorator {
        private val dates = HashSet<CalendarDay>()
        private val color = ContextCompat.getColor(context, R.color.green)

        fun addDate(day: CalendarDay) {
            dates.add(day)
        }

        fun removeDate(day: CalendarDay) {
            dates.remove(day)
        }

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return dates.contains(day)
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f, color))
            ContextCompat.getDrawable(requireContext(), R.drawable.selected_day_background)
                ?.let { view.setBackgroundDrawable(it) }
        }
    }
}