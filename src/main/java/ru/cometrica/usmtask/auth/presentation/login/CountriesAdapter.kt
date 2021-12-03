package ru.cometrica.usmtask.auth.presentation.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.cometrica.data.data.Country
import ru.cometrica.usmtask.auth.databinding.BottomSheetsCountriesItemBinding

class CountriesAdapter(
    private var countries: List<Country>
) : RecyclerView.Adapter<CountriesAdapter.ViewHolder>() {

    private lateinit var inflater: LayoutInflater
    var listener: ((Int) -> Unit)? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        inflater = LayoutInflater.from(recyclerView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(BottomSheetsCountriesItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvCountryName.text = countries[position].name
        holder.binding.tvCountryIcon.text = countries[position].flag

        holder.binding.llCountryItemContainer.setOnClickListener {
            listener?.invoke(position)
        }
    }

    fun setOnClickListener(listener: (Int) -> Unit) {
        this.listener = listener
    }

    override fun getItemCount(): Int {
        return countries.size
    }

    class ViewHolder(val binding: BottomSheetsCountriesItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setCountries(countries: List<Country>) {
        this.countries = countries
        notifyDataSetChanged()
    }
}