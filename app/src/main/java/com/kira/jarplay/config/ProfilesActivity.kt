package com.kira.jarplay.config

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.kira.jarplay.R
import com.kira.jarplay.ui.glass.rememberGlassPageBackdrop
import com.kira.jarplay.ui.glass.standaloneContentPadding
import com.kira.jarplay.ui.theme.JarPlayTheme
import com.kira.jarplay.util.Constants.ACTION_EDIT_PROFILE
import com.kira.jarplay.util.Constants.PREF_DEFAULT_PROFILE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.glass.MiuixGlassIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

class ProfilesViewModel : ViewModel() {
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _defaultProfileName = MutableStateFlow<String?>(null)
    val defaultProfileName: StateFlow<String?> = _defaultProfileName.asStateFlow()

    private lateinit var preferences: SharedPreferences

    fun init(prefs: SharedPreferences) {
        preferences = prefs
        loadProfiles()
    }

    fun loadProfiles() {
        val list = ProfilesManager.getProfiles()
        _profiles.value = list
        _defaultProfileName.value = preferences.getString(PREF_DEFAULT_PROFILE, null)
    }

    fun setDefault(profile: Profile) {
        preferences.edit().putString(PREF_DEFAULT_PROFILE, profile.name).apply()
        _defaultProfileName.value = profile.name
    }

    fun renameProfile(profile: Profile, newName: String) {
        val oldName = profile.name
        val success = profile.renameTo(newName)
        if (success) {
            _profiles.value = _profiles.value.toList()
            if (_defaultProfileName.value == oldName) {
                preferences.edit().putString(PREF_DEFAULT_PROFILE, newName).apply()
                _defaultProfileName.value = newName
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        profile.delete()
        _profiles.value = _profiles.value.filter { it.name != profile.name }
        if (_defaultProfileName.value == profile.name) {
            preferences.edit().remove(PREF_DEFAULT_PROFILE).apply()
            _defaultProfileName.value = null
        }
    }
}

class ProfilesActivity : FragmentActivity(), EditNameDialog.Callback {

    private val viewModel: ProfilesViewModel by viewModels()

    private var pendingRenameProfile: Profile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        viewModel.init(preferences)

        setContent {
            JarPlayTheme {
                ProfilesScaffold(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onShowCreateDialog = { showCreateDialog() },
                    onShowRenameDialog = { profile -> showRenameDialog(profile) },
                )
            }
        }
    }

    private fun showCreateDialog() {
        pendingRenameProfile = null
        EditNameDialog.newInstance(getString(R.string.enter_name), -1, null)
            .show(supportFragmentManager, "alert_create_profile")
    }

    private fun showRenameDialog(profile: Profile) {
        pendingRenameProfile = profile
        EditNameDialog.newInstance(getString(R.string.enter_new_name), -1, profile.name)
            .show(supportFragmentManager, "alert_rename_profile")
    }

    fun launchEditProfile(profile: Profile) {
        val intent = Intent(ACTION_EDIT_PROFILE, Uri.parse(profile.name), applicationContext, ConfigActivity::class.java)
        startActivity(intent)
    }

    override fun onNameChanged(id: Int, newName: String) {
        val profile = pendingRenameProfile
        if (profile != null) {
            viewModel.renameProfile(profile, newName)
            pendingRenameProfile = null
        } else {
            val newProfile = Profile(newName)
            newProfile.create()
            viewModel.loadProfiles()
            launchEditProfile(newProfile)
        }
    }
}

@Composable
private fun ProfilesScaffold(
    viewModel: ProfilesViewModel,
    onBack: () -> Unit,
    onShowCreateDialog: () -> Unit,
    onShowRenameDialog: (Profile) -> Unit,
) {
    val profiles by viewModel.profiles.collectAsState()
    val defaultProfileName by viewModel.defaultProfileName.collectAsState()
    val context = LocalContext.current
    val activity = context as? ProfilesActivity
    val glassPage = rememberGlassPageBackdrop()
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            GlassTopAppBar(
                title = stringResource(R.string.profiles),
                backdrop = glassPage.backdrop,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixGlassIcons.ChevronBackward,
                            contentDescription = stringResource(R.string.close),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShowCreateDialog) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = stringResource(R.string.add_profile_description)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .then(glassPage.contentModifier)
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
        ) {
            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = MiuixIcons.More,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_data_for_display),
                        style = MiuixTheme.textStyles.main,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = standaloneContentPadding(paddingValues, start = 12.dp, end = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(profiles) { profile ->
                    val isDefault = profile.name == defaultProfileName
                    ProfileItemCard(
                        profile = profile,
                        isDefault = isDefault,
                        onClick = {
                            activity?.launchEditProfile(profile)
                        },
                        onSetDefault = { viewModel.setDefault(profile) },
                        onRename = { onShowRenameDialog(profile) },
                        onDelete = { viewModel.deleteProfile(profile) },
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

}

@Composable
private fun ProfileItemCard(
    profile: Profile,
    isDefault: Boolean,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val setDefaultLabel = stringResource(R.string.set_as_default)
    val editLabel = stringResource(R.string.edit)
    val renameLabel = stringResource(R.string.action_context_rename)
    val deleteLabel = stringResource(R.string.action_context_delete)

    val menuEntry = remember(isDefault, setDefaultLabel, editLabel, renameLabel, deleteLabel) {
        DropdownEntry(
            items = buildList {
                if (!isDefault) {
                    add(DropdownItem(text = setDefaultLabel, onClick = onSetDefault))
                }
                add(DropdownItem(text = editLabel, onClick = onClick))
                add(DropdownItem(text = renameLabel, onClick = onRename))
                add(DropdownItem(text = deleteLabel, onClick = onDelete))
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(12.dp),
        colors = CardDefaults.defaultColors(),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.name,
                        style = MiuixTheme.textStyles.title4,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.default_label, profile.name),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.profiles_description),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OverlayIconDropdownMenu(
                entry = menuEntry,
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = stringResource(R.string.more_options)
                )
            }
        }
    }
}
