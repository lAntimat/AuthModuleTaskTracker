package ru.cometrica.usmtask.auth.presentation.customviews

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.cometrica.data.data.Country
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.databinding.BottomSheetsCountriesBinding
import ru.cometrica.usmtask.auth.presentation.login.CountriesAdapter
import ru.cometrica.usmtask.auth.presentation.login.LoginViewModel


class BottomSheetCountries : BottomSheetDialogFragment() {

    private val binding: BottomSheetsCountriesBinding by viewBinding()
    private var countriesAdapter: CountriesAdapter = CountriesAdapter(listOf())

    private val args by lazy { (arguments?.getParcelableArrayList<Country>(COUNTRIES) as ArrayList<Country>) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.ThemeOverlay_BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheets_countries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val linearLayoutManager = LinearLayoutManager(
            context,
            RecyclerView.VERTICAL,
            false
        )

        binding.rvCountriesList.layoutManager = linearLayoutManager
        binding.rvCountriesList.adapter = countriesAdapter
        binding.rvCountriesList.addItemDecoration(
            DividerItemDecoration(
                context,
                linearLayoutManager.orientation
            )
        )

        countriesAdapter.setCountries(args)

        binding.tvClose.setOnClickListener {
            dismiss()
        }
    }

    fun setOnClickListener(listener: (Int) -> Unit) {
        countriesAdapter.setOnClickListener {
            listener.invoke(it)
        }
    }

    companion object {
        const val COUNTRIES = "countries"

        @JvmStatic
        fun newInstance(countries: ArrayList<Country>) =
            BottomSheetCountries().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(COUNTRIES, countries)
                }
            }
    }
}