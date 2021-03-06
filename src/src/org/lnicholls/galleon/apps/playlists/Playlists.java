package org.lnicholls.galleon.apps.playlists;

/*
 * Copyright (C) 2005 Leon Nicholls
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * See the file "COPYING" for more details.
 */

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.media.Playlist;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.Lyrics;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.Yahoo;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.util.FileSystemContainer.PlaylistItem;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultPlayer;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.MusicInfo;
import org.lnicholls.galleon.widget.MusicPlayer;
import org.lnicholls.galleon.widget.ScreenSaver;
import org.lnicholls.galleon.widget.ScrollText;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;
import org.lnicholls.galleon.winamp.WinampPlayer;

import com.tivo.hme.bananas.BButton;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.interfaces.IArgumentList;

public class Playlists extends DefaultApplication {

	private static Logger log = Logger.getLogger(Playlists.class.getName());

	public final static String TITLE = "Playlists";

	private Resource mMenuBackground;

	private Resource mInfoBackground;

	private Resource mPlayerBackground;

	private Resource mLyricsBackground;

	private Resource mImagesBackground;

	private Resource mItemIcon;

	public void init(IContext context) throws Exception {
		super.init(context);

		mMenuBackground = getSkinImage("menu", "background");
		mInfoBackground = getSkinImage("info", "background");
		mPlayerBackground = getSkinImage("player", "background");
		mLyricsBackground = getSkinImage("lyrics", "background");
		mImagesBackground = getSkinImage("images", "background");
		mItemIcon = getSkinImage("menu", "item");

		PlaylistsConfiguration playlistsConfiguration = (PlaylistsConfiguration) ((PlaylistsFactory) getFactory()).getAppContext().getConfiguration();

		ArrayList list = new ArrayList();
		for (Iterator i = playlistsConfiguration.getPlaylists().iterator(); i.hasNext(); /* Nothing */) {
			NameValue nameValue = (NameValue) i.next();
			list.add(new PlaylistItem(nameValue.getName(), new File(nameValue.getValue())));
		}

		Tracker tracker = new Tracker(list, 0);
		push(new PathScreen(this, tracker, true), TRANSITION_NONE);

		initialize();
	}

	public class PathScreen extends DefaultMenuScreen {

		public PathScreen(Playlists app, Tracker tracker, boolean first) {
			super(app, "Playlists");

			mFirst = first;

			getBelow().setResource(mMenuBackground);

			mTracker = tracker;

			Iterator iterator = mTracker.getList().iterator();
			while (iterator.hasNext()) {
				Item nameFile = (Item) iterator.next();
				mMenuList.add(nameFile);
			}
		}

		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			mFocus = mTracker.getPos();
			mTracker = (Tracker)mTracker.clone();
			return super.handleEnter(arg, isReturn);
		}

		public boolean handleAction(BView view, Object action) {
			if (action.equals("push")) {
				if (mMenuList.size() > 0) {
					load();
					final Item nameFile = (Item) (mMenuList.get(mMenuList.getFocus()));
					if (nameFile.isPlaylist()) {
						try {
							mTracker.setPos(mMenuList.getFocus());
							File file = (File) nameFile.getValue();
							Playlist playlist = (Playlist) MediaManager.getMedia(file.getCanonicalPath());
							if (playlist != null && playlist.getList() != null) {
								Tracker tracker = new Tracker(playlist.getList(), 0);
								PathScreen pathScreen = new PathScreen((Playlists) getBApp(), tracker, false);
								getBApp().push(pathScreen, TRANSITION_LEFT);
								getBApp().flush();
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} else {
						new Thread() {
							public void run() {
								try {
									mTracker.setPos(mMenuList.getFocus());
									MusicScreen musicScreen = new MusicScreen((Playlists) getBApp());
									musicScreen.setTracker(mTracker);

									getBApp().push(musicScreen, TRANSITION_LEFT);
									getBApp().flush();
								} catch (Exception ex) {
									Tools.logException(Playlists.class, ex);
								}
							}
						}.start();
					}
					return true;
				}
			} else if (action.equals("play")) {
				if (mMenuList.size() > 0) {
					load();
					final Item nameFile = (Item) (mMenuList.get(mMenuList.getFocus()));
					new Thread() {
						public void run() {
							try {
								mTracker.setPos(mMenuList.getFocus());
								File file = (File) nameFile.getValue();
								Playlist playlist = (Playlist) MediaManager.getMedia(file.getCanonicalPath());
								if (playlist != null && playlist.getList() != null) {
									Tracker tracker = new Tracker(playlist.getList(), 0);
									MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
											.getMusicPlayerConfiguration();
									tracker.setRandom(musicPlayerConfiguration.isRandomPlayFolders());
									getBApp().push(new PlayerScreen((Playlists) getBApp(), tracker), TRANSITION_LEFT);
									getBApp().flush();
								}
							} catch (Exception ex) {
								Tools.logException(Playlists.class, ex);
							}
						}
					}.start();
				}
			}
			return super.handleAction(view, action);
		}

		protected void createRow(BView parent, int index) {
			BView icon = new BView(parent, 9, 2, 32, 32);
			Item nameFile = (Item) mMenuList.get(index);
			icon.setResource(mItemIcon);

			BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
			name.setShadow(true);
			name.setFlags(RSRC_HALIGN_LEFT);
			name.setValue(Tools.trim(Tools.clean(nameFile.getName()), 40));
		}

		public boolean handleKeyPress(int code, long rawcode) {
			switch (code) {
			case KEY_REPLAY:
				if (((Playlists) getBApp()).getTracker() != null) {
					new Thread() {
						public void run() {
							getBApp().push(new PlayerScreen((Playlists) getBApp(), ((Playlists) getBApp()).getTracker()),
									TRANSITION_LEFT);
							getBApp().flush();
						}
					}.start();
				}
				return true;
			case KEY_LEFT:
				if (!mFirst)
				{
					postEvent(new BEvent.Action(this, "pop"));
					return true;
				}
				break;
			}
			return super.handleKeyPress(code, rawcode);
		}

		private Tracker mTracker;

		private boolean mFirst;
	}

	public class MusicScreen extends DefaultScreen {

		private BList list;

		public MusicScreen(Playlists app) {
			super(app, "Song", true);

			getBelow().setResource(mInfoBackground);

			mMusicInfo = new MusicInfo(this.getNormal(), BORDER_LEFT, TOP, BODY_WIDTH, BODY_HEIGHT, true);

			list = new DefaultOptionList(this.getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 80,
					(int) Math.round((getWidth() - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
			list.add("Play");
			list.add("Don't do anything");

			setFocusDefault(list);
		}

		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			getBelow().setResource(mInfoBackground);
			updateView();

			return super.handleEnter(arg, isReturn);
		}

		private void updateView() {
			Audio audio = currentAudio();
			if (audio != null) {
				Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
				mMusicInfo.setAudio(audio, nameFile.getName());
			}
		}

		public boolean handleExit() {
			mMusicInfo.flush();			mMusicInfo.clearResource();
			return super.handleExit();
		}

		public boolean handleKeyPress(int code, long rawcode) {
			if (mMusicInfo.handleKeyPress(code, rawcode))
				return true;
			Audio audio = currentAudio();
			switch (code) {
			case KEY_SELECT:
			case KEY_RIGHT:
			case KEY_PLAY:
				if (list.getFocus() == 0) {
					postEvent(new BEvent.Action(this, "play"));
					return true;
				} else {
					postEvent(new BEvent.Action(this, "pop"));
					return true;
				}
			case KEY_LEFT:
				postEvent(new BEvent.Action(this, "pop"));
				return true;
			case KEY_CHANNELUP:
				getBApp().play("pageup.snd");
				getBApp().flush();
				getPrevPos();
				updateView();
				return true;
			case KEY_CHANNELDOWN:
				getBApp().play("pagedown.snd");
				getBApp().flush();
				getNextPos();
				updateView();
				return true;
			case KEY_REPLAY:
				if (((Playlists) getBApp()).getTracker() != null) {
					new Thread() {
						public void run() {
							getBApp().push(new PlayerScreen((Playlists) getBApp(), ((Playlists) getBApp()).getTracker()),
									TRANSITION_LEFT);
							getBApp().flush();
						}
					}.start();
				}
				return true;
			}
			return super.handleKeyPress(code, rawcode);
		}

		public void getNextPos() {
			if (mTracker != null) {
				int pos = mTracker.getNextPos();
				Item nameFile = (Item) mTracker.getList().get(pos);
				while (nameFile.isFolder() || nameFile.isPlaylist()) {
					pos = mTracker.getNextPos();
					nameFile = (Item) mTracker.getList().get(pos);
				}
			}
		}

		public void getPrevPos() {
			if (mTracker != null) {
				int pos = mTracker.getPrevPos();
				Item nameFile = (Item) mTracker.getList().get(pos);
				while (nameFile.isFolder() || nameFile.isPlaylist()) {
					pos = mTracker.getPrevPos();
					nameFile = (Item) mTracker.getList().get(pos);
				}
			}
		}

		public boolean handleAction(BView view, Object action) {
			if (action.equals("play") || action.equals("push")) {

				getBApp().play("select.snd");
				getBApp().flush();

				new Thread() {
					public void run() {
						getBApp().push(new PlayerScreen((Playlists) getBApp(), mTracker), TRANSITION_LEFT);
						getBApp().flush();
					}
				}.start();
				return true;
			}

			return super.handleAction(view, action);
		}

		public void setTracker(Tracker value) {
			mTracker = value;
		}

		private Audio currentAudio() {
			if (mTracker != null) {
				try {
					Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
					if (nameFile != null) {
						if (nameFile.isFile() && nameFile.getValue()!=null)
							return getAudio(((File) nameFile.getValue()).getCanonicalPath());
						else
							return getAudio((String) nameFile.getValue());
					}
				} catch (Exception ex) {
					Tools.logException(Playlists.class, ex);
				}
			}
			return null;
		}

		private MusicInfo mMusicInfo;

		private Tracker mTracker;
	}

	public class PlayerScreen extends DefaultScreen {

		public PlayerScreen(Playlists app, Tracker tracker) {
			super(app, true);

			getBelow().setResource(mPlayerBackground);

			boolean sameTrack = false;
			DefaultApplication defaultApplication = (DefaultApplication) getApp();
			Audio currentAudio = defaultApplication.getCurrentAudio();
			Tracker currentTracker = defaultApplication.getTracker();
			if (currentTracker != null && currentAudio != null) {
				Item newItem = (Item) tracker.getList().get(tracker.getPos());
				if (currentAudio.getPath().equals(newItem.getValue().toString())) {
					mTracker = currentTracker;
					sameTrack = true;
				} else {
					mTracker = tracker;
					app.setTracker(mTracker);
				}
			} else {
				mTracker = tracker;
				app.setTracker(mTracker);
			}

			setTitle(" ");

			setFooter("Press INFO for lyrics, REPLAY to return to this screen");

			if (!sameTrack || getPlayer().getState() == Player.STOP)
				getPlayer().startTrack();
		}

		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			new Thread() {
				public void run() {
					mBusy.setVisible(true);
					mBusy.flush();

					synchronized (this) {
						try {
							setPainting(false);
							MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
									.getMusicPlayerConfiguration();
							if (musicPlayerConfiguration.getPlayer().equals(MusicPlayerConfiguration.CLASSIC))
								player = new MusicPlayer(PlayerScreen.this, BORDER_LEFT, SAFE_TITLE_H, BODY_WIDTH,
										BODY_HEIGHT, false, (DefaultApplication) getApp(), mTracker);
							else
								player = new WinampPlayer(PlayerScreen.this, 0, 0, PlayerScreen.this.getWidth(),
										PlayerScreen.this.getHeight(), false, (DefaultApplication) getApp(), mTracker);
							player.updatePlayer();
							player.setVisible(true);
						} finally {
							setPainting(true);
						}
					}
					setFocusDefault(player);
					setFocus(player);
					mBusy.setVisible(false);

					MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
							.getMusicPlayerConfiguration();
					if (musicPlayerConfiguration.isScreensaver()) {
						mScreenSaver = new ScreenSaver(PlayerScreen.this);
						mScreenSaver.start();
					}
					getBApp().flush();
				}

				public void interrupt() {
					synchronized (this) {
						super.interrupt();
					}
				}
			}.start();

			return super.handleEnter(arg, isReturn);
		}

		public boolean handleExit() {
			try {
				setPainting(false);

				if (mScreenSaver != null && mScreenSaver.isAlive()) {
					mScreenSaver.interrupt();
					mScreenSaver = null;
				}
				if (player != null) {
					player.stopPlayer();
					player.setVisible(false);
					player.flush();					player.remove();
					player = null;
				}
			} finally {
				setPainting(true);
			}
			return super.handleExit();
		}

		public boolean handleKeyPress(int code, long rawcode) {
			if (mScreenSaver != null)
				mScreenSaver.handleKeyPress(code, rawcode);
			switch (code) {
			case KEY_INFO:
			case KEY_NUM0:
				getBApp().play("select.snd");
				getBApp().flush();
				LyricsScreen lyricsScreen = new LyricsScreen((Playlists) getBApp(), mTracker);
				getBApp().push(lyricsScreen, TRANSITION_LEFT);
				getBApp().flush();
				return true;
			}
			return super.handleKeyPress(code, rawcode);
		}

		private DefaultPlayer player;

		private Tracker mTracker;

		private ScreenSaver mScreenSaver;
	}

	public class LyricsScreen extends DefaultScreen {
		private BList list;

		public LyricsScreen(Playlists app, Tracker tracker) {
			super(app, "Lyrics", false);

			getBelow().setResource(mLyricsBackground);

			mTracker = tracker;

			scrollText = new ScrollText(getNormal(), BORDER_LEFT, TOP, BODY_WIDTH - 10, getHeight() - SAFE_TITLE_V
					- TOP - 70, "");
			scrollText.setVisible(false);

			// setFocusDefault(scrollText);

			// setFooter("lyrc.com.ar");
			setFooter("lyrictracker.com");

			mBusy.setVisible(true);

			/*
			 * list = new DefaultOptionList(this.getNormal(), SAFE_TITLE_H + 10,
			 * (getHeight() - SAFE_TITLE_V) - 60, (int) Math .round((getWidth() -
			 * (SAFE_TITLE_H * 2)) / 2), 90, 35);
			 * //list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, H_LEFT, null);
			 * list.add("Back to player"); setFocusDefault(list);
			 */

			BButton button = new BButton(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 40, (int) Math
					.round((getWidth() - (SAFE_TITLE_H * 2)) / 2), 35);
			button.setResource(createText("default-24.font", Color.white, "Return to player"));
			button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, null, true);
			setFocus(button);
		}

		public void updateLyrics() {
			try {
				setPainting(false);
				if (mLyricsThread != null && mLyricsThread.isAlive())
					mLyricsThread.interrupt();
			} finally {
				setPainting(true);
			}
			Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
			Audio audio = null;
			try {
				List list = null;
				if (nameFile.isFile())
					list = AudioManager.findByPath(((File) nameFile.getValue()).getCanonicalPath());
				else
					list = AudioManager.findByPath((String) nameFile.getValue());
				if (list != null && list.size() > 0) {
					audio = (Audio) list.get(0);
				}
			} catch (Exception ex) {
				Tools.logException(Playlists.class, ex);
			}

			if (audio != null) {
				if (audio.getLyrics() != null && audio.getLyrics().length() > 0) {
					try {
						setPainting(false);
						mBusy.setVisible(false);
						getBApp().flush();
						scrollText.setVisible(true);
						scrollText.setText(audio.getLyrics());
						getBApp().flush();
					} finally {
						setPainting(true);
					}
				} else {
					final Audio lyricsAudio = audio;

					mLyricsThread = new Thread() {
						public void run() {
							try {
								String lyrics = Lyrics.getLyrics(lyricsAudio.getTitle(), lyricsAudio.getArtist());
								if (lyrics == null || lyrics.trim().length() == 0) {
									lyrics = "Lyrics not found";
								} else {
									synchronized (this) {
										try {
											lyricsAudio.setLyrics(lyrics);
											AudioManager.updateAudio(lyricsAudio);
										} catch (Exception ex) {
											Tools.logException(Playlists.class, ex, "Could not update lyrics");
										}
									}
								}
								synchronized (this) {
									try {
										setPainting(false);
										mBusy.setVisible(false);
										getBApp().flush();
										scrollText.setVisible(true);
										scrollText.setText(lyrics);
										getBApp().flush();
									} finally {
										setPainting(true);
									}
								}
							} catch (Exception ex) {
								Tools.logException(Playlists.class, ex, "Could retrieve lyrics");
							}
						}

						public void interrupt() {
							synchronized (this) {
								super.interrupt();
							}
						}
					};
					mLyricsThread.start();
				}
			}
		}

		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			updateLyrics();

			return super.handleEnter(arg, isReturn);
		}

		public boolean handleExit() {
			try {
				setPainting(false);
				if (mLyricsThread != null && mLyricsThread.isAlive()) {
					mLyricsThread.interrupt();
					mLyricsThread = null;
				}
			} finally {
				setPainting(true);
			}
			return super.handleExit();
		}

		public boolean handleKeyPress(int code, long rawcode) {
			switch (code) {
			case KEY_SELECT:
				postEvent(new BEvent.Action(this, "pop"));
				return true;
			case KEY_UP:
			case KEY_DOWN:
			case KEY_CHANNELUP:
			case KEY_CHANNELDOWN:
				scrollText.handleKeyPress(code, rawcode);
				return true;
			}
			return super.handleKeyPress(code, rawcode);
		}

		private ScrollText scrollText;

		private Thread mLyricsThread;

		private Tracker mTracker;
	}

	public class ImagesScreen extends DefaultScreen {
		private BList list;

		public ImagesScreen(Playlists app, Tracker tracker) {
			super(app, "Images", true);

			getBelow().setResource(mImagesBackground);

			mTracker = tracker;

			mImageView = new BView(this.getNormal(), BORDER_LEFT, TOP, BODY_WIDTH, getHeight() - SAFE_TITLE_V - TOP
					- 75);
			mImageView.setVisible(false);

			mPosText = new BText(getNormal(), BORDER_LEFT, getHeight() - SAFE_TITLE_V - 60, BODY_WIDTH, 30);
			mPosText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
			mPosText.setFont("default-18-bold.font");
			mPosText.setColor(Color.CYAN);
			mPosText.setShadow(true);

			mUrlText = new BText(getNormal(), SAFE_TITLE_H, getHeight() - SAFE_TITLE_V - 78, BODY_WIDTH, 15);
			mUrlText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_BOTTOM);
			mUrlText.setFont("default-12-bold.font");
			mUrlText.setColor(Color.WHITE);
			mUrlText.setShadow(true);

			setFooter("search.yahoo.com");

			mBusy.setVisible(true);

			/*
			 * list = new DefaultOptionList(this.getNormal(), SAFE_TITLE_H + 10,
			 * (getHeight() - SAFE_TITLE_V) - 60, (int) Math .round((getWidth() -
			 * (SAFE_TITLE_H * 2)) / 2), 90, 35);
			 * //list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, H_LEFT, null);
			 * list.add("Back to player"); setFocusDefault(list);
			 */

			BButton button = new BButton(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 55, (int) Math
					.round((getWidth() - (SAFE_TITLE_H * 2)) / 2), 35);
			button.setResource(createText("default-24.font", Color.white, "Return to player"));
			button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, null, true);
			setFocus(button);
		}

		public void updateImage() {
			Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
			Audio audio = null;
			try {
				List list = null;
				if (nameFile.isFile())
					list = AudioManager.findByPath(((File) nameFile.getValue()).getCanonicalPath());
				else
					list = AudioManager.findByPath((String) nameFile.getValue());
				if (list != null && list.size() > 0) {
					audio = (Audio) list.get(0);
				}
			} catch (Exception ex) {
				Tools.logException(Playlists.class, ex);
			}
			final Audio lyricsAudio = audio;

			mImageThread = new Thread() {
				public void run() {
					try {
						synchronized (this) {
							mBusy.setVisible(true);
							getBApp().flush();
						}

						if (mResults == null || mResults.size() == 0) {
							mResults = Yahoo.getImages("\"" + lyricsAudio.getArtist() + "\" music");
							mPos = 0;
						}
						if (mResults.size() == 0) {
							synchronized (this) {
								setPainting(false);
								try {
									mBusy.setVisible(false);
									getBApp().flush();
								} finally {
									setPainting(true);
								}
							}
							return;
						}

						NameValue nameValue = (NameValue) mResults.get(mPos);
						Image image = Tools.getImage(new URL(nameValue.getValue()), -1, -1);

						if (image != null) {
							synchronized (this) {
								try {
									setPainting(false);
									if (mImageView.getResource() != null)									{
										mImageView.getResource().flush();										mImageView.getResource().remove();									}
									mUrlText.setValue(nameValue.getName());
									mImageView.setVisible(true);
									mImageView.setTransparency(1f);
									mImageView.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
									mImageView.setTransparency(0f, getResource("*500"));
									image.flush();
									image = null;
								} finally {
									setPainting(true);
								}
							}
						} else {
							mResults.remove(mPos);
						}

					} catch (Exception ex) {
						Tools.logException(Playlists.class, ex, "Could not retrieve image");
						mResults.remove(mPos);
					} finally {
						synchronized (this) {
							try {
								setPainting(false);
								if (mResults != null && mResults.size() > 0)
									mPosText.setValue(String.valueOf(mPos + 1) + " of "
											+ String.valueOf(mResults.size()));
								else
									mPosText.setValue("No images found");
								mBusy.setVisible(false);
							} finally {
								setPainting(true);
							}
							getBApp().flush();
						}
					}
				}

				public void interrupt() {
					synchronized (this) {
						super.interrupt();
					}
				}
			};
			mImageThread.start();
		}

		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			updateImage();

			return super.handleEnter(arg, isReturn);
		}

		public boolean handleExit() {
			try {
				setPainting(false);
				if (mImageThread != null && mImageThread.isAlive()) {
					mImageThread.interrupt();
					mImageThread = null;
					mResults.clear();
					mResults = null;
				}
			} finally {
				setPainting(true);
			}
			return super.handleExit();
		}

		public boolean handleKeyPress(int code, long rawcode) {
			switch (code) {
			case KEY_SELECT:
				postEvent(new BEvent.Action(this, "pop"));
				return true;
			case KEY_UP:
			case KEY_DOWN:
			case KEY_CHANNELUP:
				if (mResults != null && mResults.size() > 0) {
					getBApp().play("pageup.snd");
					getBApp().flush();
					mPos = mPos - 1;
					if (mPos == -1)
						mPos = mResults.size() - 1;
				}
				updateImage();
				return true;
			case KEY_CHANNELDOWN:
				if (mResults != null && mResults.size() > 0) {
					getBApp().play("pagedown.snd");
					getBApp().flush();
					mPos = mPos + 1;
					if (mPos == mResults.size())
						mPos = 0;
				}
				updateImage();
				return true;
			}
			return super.handleKeyPress(code, rawcode);
		}

		private BView mImageView;

		private Thread mImageThread;

		private Tracker mTracker;

		private List mResults;

		private int mPos;

		private BText mPosText;

		private BText mUrlText;
	}

	private static Audio getAudio(String path) {
		Audio audio = null;
		try {
			List list = AudioManager.findByPath(path);
			if (list != null && list.size() > 0) {
				audio = (Audio) list.get(0);
			}
		} catch (Exception ex) {
			Tools.logException(Playlists.class, ex);
		}

		if (audio == null) {
			try {
				audio = (Audio) MediaManager.getMedia(path);
				AudioManager.createAudio(audio);
			} catch (Exception ex) {
				Tools.logException(Playlists.class, ex);
			}
		}
		return audio;
	}

	public static class PlaylistsFactory extends AppFactory {

		public void initialize() {
			PlaylistsConfiguration playlistsConfiguration = (PlaylistsConfiguration) getAppContext().getConfiguration();

			MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer().getMusicPlayerConfiguration();
		}
	}
}