package com.minseok.tel

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import androidx.core.content.ContextCompat

class MessageBusinessTrip : Fragment(), OnDateSelectedListener {
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var selectedDatesText: TextView
    private lateinit var selectedDateDecorator: SelectedDateDecorator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message_business_trip, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        selectedDatesText = view.findViewById(R.id.selectedDatesText)

        calendarView.setOnDateChangedListener(this)
        calendarView.selectionMode = MaterialCalendarView.SELECTION_MODE_MULTIPLE

        // 선택된 날짜 데코레이터 초기화 및 추가
        selectedDateDecorator = SelectedDateDecorator(requireContext())
        calendarView.addDecorator(selectedDateDecorator)

        // 오늘 날짜 강조 표시
        calendarView.addDecorator(TodayDecorator())

        return view
    }

    override fun onDateSelected(
        widget: MaterialCalendarView,
        date: CalendarDay,
        selected: Boolean
    ) {
        if (selected) {
            selectedDateDecorator.addDate(date)
        } else {
            selectedDateDecorator.removeDate(date)
        }
        calendarView.invalidateDecorators()
        updateSelectedDatesText()
    }

    private fun updateSelectedDatesText() {
        val selectedDates = calendarView.selectedDates
        val dateStrings = selectedDates.map { "${it.year}년 ${it.month}월 ${it.day}일" }
        selectedDatesText.text = "${dateStrings.joinToString(", ")}"
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