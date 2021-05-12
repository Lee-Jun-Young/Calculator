package com.example.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.item_history.view.*
import java.lang.NumberFormatException

// TODO 계산 기록 결과 수정, 계산 기능 추가

class MainActivity : AppCompatActivity() {

    private var isOperator = false
    private var hasOperator = false

    lateinit var db : AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "historyDB"
        ).build()

    }

    fun buttonClicked(v: View){
        when(v.id){
            R.id.btn_one -> numberButtonClicked("1")
            R.id.btn_two -> numberButtonClicked("2")
            R.id.btn_three -> numberButtonClicked("3")
            R.id.btn_four -> numberButtonClicked("4")
            R.id.btn_five -> numberButtonClicked("5")
            R.id.btn_six -> numberButtonClicked("6")
            R.id.btn_seven -> numberButtonClicked("7")
            R.id.btn_eight -> numberButtonClicked("8")
            R.id.btn_nine -> numberButtonClicked("9")
            R.id.btn_zero -> numberButtonClicked("0")
            R.id.btn_plus -> operatorButtonClicked("+")
            R.id.btn_minus -> operatorButtonClicked("-")
            R.id.btn_mul -> operatorButtonClicked("*")
            R.id.btn_div -> operatorButtonClicked("/")
            R.id.btn_rest -> operatorButtonClicked("%")
        }
    }

    private fun numberButtonClicked(num : String){

        if(isOperator){
            tv_textView.append(" ")
        }
        isOperator = false

        val expressionText = tv_textView.text .split(" ")
        if(expressionText.isNotEmpty() && expressionText.last().length >= 15){
            Toast.makeText(this, "15자리 까지만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }else if(expressionText.last().isEmpty() && num == "0"){
            Toast.makeText(this, "첫번쨰 숫자는 0이 될 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        tv_textView.append(num)
        tv_result.text = calculateExpression()
    }

    private fun operatorButtonClicked(operator : String){
        if(tv_textView.text.isEmpty()){
            return
        }

        when{
            isOperator -> {
                val text = tv_textView.text.toString()
                tv_textView.text = text.dropLast(1) + operator
            }
            hasOperator -> {
                Toast.makeText(this, "연산자는 한 번만 사용할 수 있습니다. ", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                tv_textView.append(" $operator")
            }
        }

        val ssb = SpannableStringBuilder(tv_textView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            tv_textView.text.length - 1,
            tv_textView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        tv_textView.text = ssb

        isOperator = true
        hasOperator = true
    }

    fun clearButtonClicked(v: View){
        tv_textView.text = ""
        tv_result.text = ""
        isOperator = false
        hasOperator = false
    }

    fun resultButtonClicked(v: View){
        val expressionTexts = tv_textView.text .split(" ")

        if(tv_textView.text.isEmpty() || expressionTexts.size == 1){
            return
        }

        if(expressionTexts.size != 3 && hasOperator){
            Toast.makeText(this, "수식을 완성해 주십시오. ", Toast.LENGTH_SHORT).show()
            return
        }

        if(expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            Toast.makeText(this, "오류가 발생했습니다. ", Toast.LENGTH_SHORT).show()
            return
        }

        val expressionText = tv_textView.text.toString()
        val resultText = calculateExpression()

        Thread(Runnable {
            db.historyDao().insertHistory(History(null, expressionText, resultText))
        }).start()

        tv_result.text = ""
        tv_textView.text = resultText

        isOperator = false
        hasOperator = false

    }

    private fun calculateExpression() : String{
        val expressionTexts = tv_textView.text .split(" ")

        if(!hasOperator || expressionTexts.size != 3){
            return ""
        }else if(expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            return ""
        }

        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()

        return when(expressionTexts[1]){
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }
    }

    fun historyButtonClicked(v: View){
        historyLayout.isVisible = true

        historyLinearLayout.removeAllViews()

        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach {
                runOnUiThread {
                    val historyView = LayoutInflater.from(this).inflate(R.layout.item_history, null, false)
                    historyView.tv_result1.text = it.expression
                    historyView.tv_result2.text = it.result

                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()
    }

    fun closeHistoryButtonClicked(v: View){
        historyLayout.isVisible = false
    }

    fun deleteHistoryButtonClicked(v: View){
        historyLinearLayout.removeAllViews()
        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()
    }
}

fun String.isNumber() : Boolean{
    return try{
        this.toBigInteger()
        true
    }catch (e: NumberFormatException){
        false
    }
}