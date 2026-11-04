package org.fossify.phone.activities

import android.os.Bundle
import com.google.android.material.textfield.TextInputEditText
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.PERMISSION_READ_PHONE_STATE
import org.fossify.commons.models.RadioItem
import org.fossify.phone.R
import org.fossify.phone.databinding.ActivityManageSimBinding
import org.fossify.phone.databinding.ItemSimPrefixesBinding
import org.fossify.phone.extensions.config
import org.fossify.phone.extensions.getAvailableSIMCardLabels
import org.fossify.phone.helpers.SIM_MODE_ASK_AND_REMEMBER
import org.fossify.phone.helpers.SIM_MODE_MAIN
import org.fossify.phone.helpers.SIM_MODE_PREFIXES
import org.fossify.phone.models.SIMAccount

class ManageSimActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityManageSimBinding::inflate)

    private var sims = listOf<SIMAccount>()
    private val prefixInputs = mutableMapOf<String, TextInputEditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            setupEdgeToEdge(padBottomSystem = listOf(manageSimScrollview))
            setupMaterialScrollListener(manageSimScrollview, manageSimAppbar)
        }

        handlePermission(PERMISSION_READ_PHONE_STATE) { granted ->
            sims = if (granted) getAvailableSIMCardLabels().sortedBy { it.id } else emptyList()
            setupViews()
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.manageSimAppbar, NavigationIcon.Arrow)
    }

    override fun onPause() {
        super.onPause()
        savePrefixes()
    }

    private fun setupViews() {
        binding.manageSimFewerThanTwo.beVisibleIf(sims.size < 2)
        setupMode()
        setupMainSim()
        setupPrefixes()
        updateTextColors(binding.manageSimHolder)
        binding.manageSimPrefixesLabel.setTextColor(getProperPrimaryColor())
    }

    private fun setupMode() {
        binding.manageSimMode.text = getModeText()
        binding.manageSimPrefixesSection.beVisibleIf(config.simSelectionMode == SIM_MODE_PREFIXES)
        binding.manageSimModeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(SIM_MODE_MAIN, getString(R.string.sim_mode_main)),
                RadioItem(SIM_MODE_ASK_AND_REMEMBER, getString(R.string.sim_mode_ask_and_remember)),
                RadioItem(SIM_MODE_PREFIXES, getString(R.string.sim_mode_prefixes))
            )

            RadioGroupDialog(this, items, config.simSelectionMode) {
                config.simSelectionMode = it as Int
                binding.manageSimMode.text = getModeText()
                binding.manageSimPrefixesSection.beVisibleIf(it == SIM_MODE_PREFIXES)
            }
        }
    }

    private fun getModeText() = getString(
        when (config.simSelectionMode) {
            SIM_MODE_ASK_AND_REMEMBER -> R.string.sim_mode_ask_and_remember
            SIM_MODE_PREFIXES -> R.string.sim_mode_prefixes
            else -> R.string.sim_mode_main
        }
    )

    private fun setupMainSim() {
        binding.manageSimMain.text = getMainSimText()
        binding.manageSimMainHolder.setOnClickListener {
            val items = arrayListOf(RadioItem(0, getString(R.string.system_default_sim), ""))
            sims.forEach { sim ->
                items.add(RadioItem(sim.id, getSimTitle(sim), sim.handle.id))
            }

            val checkedId = sims.firstOrNull { it.handle.id == config.mainSimId }?.id ?: 0
            RadioGroupDialog(this, items, checkedId) {
                config.mainSimId = it as String
                binding.manageSimMain.text = getMainSimText()
            }
        }
    }

    private fun getMainSimText(): String {
        val mainSim = sims.firstOrNull { it.handle.id == config.mainSimId }
        return if (mainSim != null) getSimTitle(mainSim) else getString(R.string.system_default_sim)
    }

    private fun setupPrefixes() {
        binding.manageSimPrefixesHolder.removeAllViews()
        prefixInputs.clear()
        sims.forEach { sim ->
            val item = ItemSimPrefixesBinding.inflate(layoutInflater, binding.manageSimPrefixesHolder, true)
            item.simPrefixesHint.hint = getSimTitle(sim)
            item.simPrefixes.setText(config.getSimPrefixes(sim.handle.id))
            prefixInputs[sim.handle.id] = item.simPrefixes
        }
    }

    private fun savePrefixes() {
        prefixInputs.forEach { (simId, input) ->
            config.setSimPrefixes(simId, input.text?.toString()?.trim().orEmpty())
        }
    }

    private fun getSimTitle(sim: SIMAccount) = "SIM ${sim.id} - ${sim.label}"
}
