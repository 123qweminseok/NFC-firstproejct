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
import com.minseok.tel.EncryptionUtil
import com.minseok.tel.R
import javax.crypto.spec.SecretKeySpec

class MessageBusinessTrip : Fragment(), OnDateSelectedListener, OnRangeSelectedListener {
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var selectedDatesText: TextView
    private lateinit var nameInput: EditText
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
        val view = inflater.inflate(R.layout.fragment_message_business_trip, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        selectedDatesText = view.findViewById(R.id.selectedDatesText)
        nameInput = view.findViewById(R.id.nameInput)
        submitButton = view.findViewById(R.id.submitButton)

        phoneNumber = arguments?.getString("phoneNumber") ?: ""
        secretKey = loadKey()
        database = FirebaseDatabase.getInstance().reference

        checkUserPermission { isAdmin ->
            this.isAdmin = isAdmin
            activity?.runOnUiThread {
                if (isAdmin) {
                    setupAdminView()
                } else {
                    setupUserView()
                }
            }
        }

        calendarView.setOnDateChangedListener(this)
        calendarView.setOnRangeSelectedListener(this)
        calendarView.selectionMode = MaterialCalendarView.SELECTION_MODE_MULTIPLE

        selectedDateDecorator = SelectedDateDecorator(requireContext())
        calendarView.addDecorator(selectedDateDecorator)
        calendarView.addDecorator(TodayDecorator())

        submitButton.setOnClickListener {
            val name = nameInput.text.toString()
            if (selectedDates.isNotEmpty() && name.isNotEmpty()) {
                saveBusinessTripToFirebase(selectedDates.toList(), name)
            } else {
                Toast.makeText(context, "날짜와 이름을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadKey(): SecretKeySpec {
        val fixedKeyString = "12345678901234567890123456789012" // ManagerActivity와 동일한 키
        val fixedKey = fixedKeyString.toByteArray(Charsets.UTF_8)
        return SecretKeySpec(fixedKey, "AES")
    }

    private fun checkUserPermission(callback: (Boolean) -> Unit) {
        val encryptedPhoneNumber = EncryptionUtil.encrypt(phoneNumber, secretKey)

        database.orderByChild("value").equalTo(encryptedPhoneNumber).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val encryptedPermission = userSnapshot.child("permission").getValue(String::class.java)
                        val decryptedPermission = EncryptionUtil.decrypt(encryptedPermission ?: "", secretKey)
                        callback(decryptedPermission == "admin")
                        return
                    }
                }
                callback(false) // 사용자를 찾지 못한 경우
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MessageBusinessTrip", "Error checking user permission", databaseError.toException())
                callback(false) // 오류 발생 시 기본값은 false (user)
            }
        })
    }

    private fun setupAdminView() {
        nameInput.visibility = View.VISIBLE
        submitButton.visibility = View.VISIBLE
    }

    private fun setupUserView() {
        nameInput.visibility = View.GONE
        submitButton.visibility = View.GONE
    }

    private fun saveBusinessTripToFirebase(dates: List<CalendarDay>, name: String) {
        val database = FirebaseDatabase.getInstance()
        val businessTripsRef = database.getReference("business_trips")

        dates.forEach { date ->
            val dateString = "${date.year}-${date.month}-${date.day}"
            businessTripsRef.child(dateString).push().setValue(name)
                .addOnSuccessListener {
                    Toast.makeText(context, "출장 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    nameInput.text.clear()
                    selectedDates.clear()
                    calendarView.clearSelection()
                    updateSelectedDatesText()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "저장에 실패했습니다: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDateSelected(widget: MaterialCalendarView, date: CalendarDay, selected: Boolean) {
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
            loadBusinessTripsForDate(date)
        }
        calendarView.invalidateDecorators()
    }

    override fun onRangeSelected(widget: MaterialCalendarView, dates: List<CalendarDay>) {
        selectedDates.addAll(dates)
        dates.forEach { selectedDateDecorator.addDate(it) }
        updateSelectedDatesText()
        calendarView.invalidateDecorators()
    }

    private fun loadBusinessTripsForDate(date: CalendarDay) {
        val dateString = "${date.year}-${date.month}-${date.day}"
        val database = FirebaseDatabase.getInstance()
        val businessTripsRef = database.getReference("business_trips").child(dateString)

        businessTripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val names = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                if (names.isNotEmpty()) {
                    selectedDatesText.text = "출장자: ${names.joinToString(", ")}"
                } else {
                    selectedDatesText.text = "해당 날짜에 출장자가 없습니다."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                selectedDatesText.text = "데이터 로딩 중 오류가 발생했습니다."
            }
        })
    }

    private fun updateSelectedDatesText() {
        val dateStrings = selectedDates.map { "${it.year}년 ${it.month}월 ${it.day}일" }
        selectedDatesText.text = "선택된 날짜: ${dateStrings.joinToString(", ")}"
    }

    // 오늘 날짜를 강조 표시하는 Decorator
    inner class TodayDecorator : DayViewDecorator {
        private val today = CalendarDay.today()

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return day == today
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(5f, ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)))
        }
    }

    // 선택된 날짜를 강조 표시하는 Decorator
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