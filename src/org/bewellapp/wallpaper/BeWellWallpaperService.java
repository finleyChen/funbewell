package org.bewellapp.wallpaper;

import org.bewellapp.ScoreComputation.ScoreComputationService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Wallpaper service. A Live Wallpaper is essentially a background service that
 * can update a surface. This wallpaper can receive broadcasts to update the
 * wallpaper look and feel. See the {@link ScoresUpdateReceiver} class.
 * 
 * @author gcardone
 * 
 */
public class BeWellWallpaperService extends GLWallpaperService {

	// change the Renderer
	private BeWellFishRenderer mRenderer;
	private ScoresUpdateReceiver mScoresUpdateReceiver;

	@Override
	public Engine onCreateEngine() {

		// register the receiver
		mScoresUpdateReceiver = new ScoresUpdateReceiver();
		registerReceiver(mScoresUpdateReceiver, new IntentFilter(ScoreComputationService.ACTION_BEWELL_LWP_UPDATE_SCORE));

		// change the Renderer
		mRenderer = new BeWellFishRenderer(this);
		return new GLEngine() {
			{
				setRenderer(mRenderer);
				setRenderMode(RENDERMODE_CONTINUOUSLY);
				setTouchEventsEnabled(true);
			}
		};
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mScoresUpdateReceiver);
	}

	/**
	 * Class that receives broadcasts containing new wellness scores and updates
	 * the wallpaper accordingly.
	 * 
	 * @author gcardone
	 * 
	 */
	class ScoresUpdateReceiver extends BroadcastReceiver {

		/**
		 * Receives broadcasts that update the wallpaper. A well-formed
		 * broadcast intent has ACTION_BEWELL_LWP_UPDATE_SCORE as intent action.
		 * It also has a bundle that contains:
		 * <ul>
		 * <li>Under the <em>BEWELL_SCORE_[PHYSICAL|SOCIAL|SLEEP]</em> key, an int
		 * representing the new score.</li>
		 * <li>Under the <em>BEWELL_SCORE_[PHYSICAL|SOCIAL|SLEEP]_OLD</em> key, an int
		 * representing the old score.</li>
		 * </ul>
		 * 
		 * @param context
		 *            Context of the broadcast
		 * @param intent
		 *            Intent of the broadcast
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ScoreComputationService.ACTION_BEWELL_LWP_UPDATE_SCORE)) {
				int physicalScore = intent.getIntExtra(ScoreComputationService.BEWELL_SCORE_PHYSICAL, 0);
				int physicalScoreOld = intent.getIntExtra(ScoreComputationService.BEWELL_SCORE_PHYSICAL_OLD, 0);
				int socialScore = intent.getIntExtra(ScoreComputationService.BEWELL_SCORE_SOCIAL, 0);
				int socialScoreOld = intent.getIntExtra(ScoreComputationService.BEWELL_SCORE_SOCIAL_OLD, 0);
				int sleepScore = intent.getIntExtra(ScoreComputationService.BEWELL_SCORE_SLEEP, 0);
				int sleepScoreOld = intent.getIntExtra(ScoreComputationService.BEWELL_SCORE_SLEEP_OLD, 0);
				mRenderer.setPhysicalActivityScore(physicalScore);
				mRenderer.setSocialActivityScore(socialScore);
				mRenderer.setSleepScore(sleepScore);
				mRenderer.mPhysActOld = physicalScoreOld;
				mRenderer.mSocialActOld = socialScoreOld;
				mRenderer.mSleepOld = sleepScoreOld;
			}
		}

	}
}
