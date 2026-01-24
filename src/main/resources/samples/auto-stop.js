/**
 * Auto-Stop System
 * Automatically stops server every hour.
 *
 */

(function () {
	"use strict";

	/** 
     * Before activating this script, please add a bash script to auto-start the server.
	 * 
	 * Example:
 	Scheduler.runRepeatingMs(
		function () {
			CommandExecutor.executeAsConsole("stop");
		},
		1000 * 60 * 60,
	);

	Logger.info("Auto-Stop script loaded.");
	*/
})();
