package com.example.surahtracker   // <- keep YOUR project's package line here

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

private const val NAMES = "Al-Fatihah,Al-Baqarah,Aali Imran,An-Nisa',Al-Ma'idah,Al-An'am,Al-A'raf,Al-Anfal,At-Taubah,Yunus," +
        "Hud,Yusuf,Ar-Ra'd,Ibrahim,Al-Hijr,An-Nahl,Al-Isra',Al-Kahf,Maryam,Ta-Ha," +
        "Al-Anbiya',Al-Haj,Al-Mu'minun,An-Nur,Al-Furqan,Ash-Shu'ara',An-Naml,Al-Qasas,Al-Ankabut,Ar-Rum," +
        "Luqman,As-Sajdah,Al-Ahzab,Saba',Al-Fatir,Ya-Sin,As-Saffah,Sad,Az-Zumar,Ghafar," +
        "Fusilat,Ash-Shura,Az-Zukhruf,Ad-Dukhan,Al-Jathiyah,Al-Ahqaf,Muhammad,Al-Fat'h,Al-Hujurat,Qaf," +
        "Adz-Dzariyah,At-Tur,An-Najm,Al-Qamar,Ar-Rahman,Al-Waqi'ah,Al-Hadid,Al-Mujadilah,Al-Hashr,Al-Mumtahanah," +
        "As-Saf,Al-Jum'ah,Al-Munafiqun,At-Taghabun,At-Talaq,At-Tahrim,Al-Mulk,Al-Qalam,Al-Haqqah,Al-Ma'arij," +
        "Nuh,Al-Jinn,Al-Muzammil,Al-Mudaththir,Al-Qiyamah,Al-Insan,Al-Mursalat,An-Naba',An-Nazi'at,'Abasa," +
        "At-Takwir,Al-Infitar,Al-Mutaffifin,Al-Inshiqaq,Al-Buruj,At-Tariq,Al-A'la,Al-Ghashiyah,Al-Fajr,Al-Balad," +
        "Ash-Shams,Al-Layl,Adh-Dhuha,Al-Inshirah,At-Tin,Al-'Alaq,Al-Qadar,Al-Bayinah,Az-Zalzalah,Al-'Adiyah," +
        "Al-Qari'ah,At-Takathur,Al-'Asr,Al-Humazah,Al-Fil,Quraish,Al-Ma'un,Al-Kauthar,Al-Kafirun,An-Nasr," +
        "Al-Masad,Al-Ikhlas,Al-Falaq,An-Nas"

// state values
private const val PENDING = 0
private const val DONE = 1
private const val PARTIAL = 2

class MainActivity : AppCompatActivity() {

    private val names = NAMES.split(",")
    private val states = IntArray(114)
    private val notes = Array(114) { "" }
    private val shown = ArrayList<Int>()      // surah indexes currently visible
    private var filter = -1                   // -1 = show all
    private val tileViews = HashMap<Int, View>()

    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: SurahAdapter
    private lateinit var tvSummary: TextView
    private lateinit var tvDone: TextView
    private lateinit var tvPartial: TextView
    private lateinit var tvPending: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        prefs = getSharedPreferences("surah_progress", MODE_PRIVATE)
        for (i in 0 until 114) {
            states[i] = prefs.getInt("s$i", PENDING)
            notes[i] = prefs.getString("n$i", "") ?: ""
        }

        tvSummary = findViewById(R.id.tvSummary)
        tvDone = findViewById(R.id.tvDone)
        tvPartial = findViewById(R.id.tvPartial)
        tvPending = findViewById(R.id.tvPending)
        progress = findViewById(R.id.progress)

        val tiles = listOf(R.id.tileDone to DONE, R.id.tilePartial to PARTIAL, R.id.tilePending to PENDING)
        for ((id, f) in tiles) {
            val v = findViewById<View>(id)
            tileViews[f] = v
            v.setOnClickListener {
                filter = if (filter == f) -1 else f   // tap again to show all
                applyFilter()
            }
        }

        adapter = SurahAdapter()
        val rv = findViewById<RecyclerView>(R.id.rvSurahs)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        applyFilter()
    }

    private fun applyFilter() {
        shown.clear()
        shown.addAll((0 until 114).filter { filter == -1 || states[it] == filter })
        adapter.notifyDataSetChanged()
        updateStats()
    }

    private fun updateStats() {
        val done = states.count { it == DONE }
        val partial = states.count { it == PARTIAL }
        tvDone.text = done.toString()
        tvPartial.text = partial.toString()
        tvPending.text = (114 - done - partial).toString()
        tvSummary.text = "$done of 114 surahs completed"
        progress.progress = done * 100 / 114
        tileViews.forEach { (f, v) -> v.alpha = if (filter == -1 || filter == f) 1f else 0.45f }
    }

    private fun setState(i: Int, s: Int) {
        states[i] = s
        prefs.edit().putInt("s$i", s).apply()
        updateStats()
    }

    private fun setNote(i: Int, t: String) {
        notes[i] = t
        prefs.edit().putString("n$i", t).apply()
    }

    inner class SurahAdapter : RecyclerView.Adapter<SurahAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val card = v as MaterialCardView
            val num: TextView = v.findViewById(R.id.tvNumber)
            val name: TextView = v.findViewById(R.id.tvName)
            val cbDone: CheckBox = v.findViewById(R.id.cbDone)
            val cbPartial: CheckBox = v.findViewById(R.id.cbPartial)
            val etNote: EditText = v.findViewById(R.id.etNote)
            var binding = false

            init {
                cbDone.setOnCheckedChangeListener { _, checked ->
                    val p = bindingAdapterPosition
                    if (binding || p == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener
                    val i = shown[p]
                    setState(i, if (checked) DONE else PENDING)
                    style(this, i)
                }
                cbPartial.setOnCheckedChangeListener { _, checked ->
                    val p = bindingAdapterPosition
                    if (binding || p == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener
                    val i = shown[p]
                    setState(i, if (checked) PARTIAL else PENDING)
                    style(this, i)
                }
                etNote.doAfterTextChanged {
                    val p = bindingAdapterPosition
                    if (binding || p == RecyclerView.NO_POSITION) return@doAfterTextChanged
                    setNote(shown[p], it.toString())
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_surah, parent, false))

        override fun getItemCount() = shown.size

        override fun onBindViewHolder(h: VH, position: Int) {
            val i = shown[position]
            h.binding = true
            h.num.text = (i + 1).toString()
            h.name.text = names[i]
            h.etNote.setText(notes[i])
            h.binding = false
            style(h, i)
        }

        private fun style(h: VH, i: Int) {
            val s = states[i]
            h.binding = true
            h.cbDone.isChecked = s == DONE
            h.cbPartial.isChecked = s == PARTIAL
            h.binding = false
            h.etNote.visibility = if (s == PARTIAL) View.VISIBLE else View.GONE
            when (s) {
                DONE -> {
                    h.card.setCardBackgroundColor(Color.parseColor("#1F7A57"))
                    h.card.strokeColor = Color.parseColor("#D4AF37")
                }
                PARTIAL -> {
                    h.card.setCardBackgroundColor(Color.parseColor("#3A5A2E"))
                    h.card.strokeColor = Color.parseColor("#F0A030")
                }
                else -> {
                    h.card.setCardBackgroundColor(Color.parseColor("#124E3A"))
                    h.card.strokeColor = Color.parseColor("#22FFFFFF")
                }
            }
        }
    }
}