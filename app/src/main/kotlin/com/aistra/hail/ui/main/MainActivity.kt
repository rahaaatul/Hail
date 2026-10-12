    override fun onDestinationChanged(
        controller: NavController, destination: NavDestination, arguments: Bundle?
    ) {
        fab.setOnClickListener(null)
        fab.tag = destination.id == R.id.nav_home || destination.id == R.id.nav_actions
        fab.setIconResource(R.drawable.ic_round_add)
        when (destination.id) {
            R.id.nav_home -> {
                fab.setText(R.string.action_add_apps)
                fab.setExtended(isLandscape)
                fab.contentDescription = getString(R.string.action_add_apps)
                fab.setOnClickListener { controller.navigate(R.id.nav_apps) }
                fab.show()
            }
            R.id.nav_actions -> {
                fab.setText(R.string.action_add_action)
                fab.setExtended(isLandscape)
                fab.contentDescription = getString(R.string.action_add_action)
                fab.setOnClickListener {
                    (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)
                        ?.childFragmentManager?.primaryNavigationFragment?.let { fragment ->
                            (fragment as? com.aistra.hail.ui.actions.ActionsFragment)?.showEditor(null)
                        }
                }
                fab.show()
            }
            else -> {
                fab.setText("")
                fab.hide()
            }
        }
        binding.bottomNav?.isVisible = destination.id != R.id.nav_about
    }
