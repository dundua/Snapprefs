/*
 * Copyright (C) 2014  Sturmen, stammler, Ramis and P1nGu1n
 *
 * This file is part of Keepchat.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.marz.snapprefs;

public class Obfuscator {
    /**
     * Check if Keepchat is compatible with this Snapchat version.
     * @param versionCode The version code of the current Snapchat version
     * @return Whether it's supported
     */
    public static boolean isSupported(int versionCode) {
        return versionCode >= SUPPORTED_VERSION_CODE;
    }

    // Keepchat supports v9.1.2.0 and up
    private static final int SUPPORTED_VERSION_CODE = 531;

    // com.snapchat.model.Snap
    // Snap.getTimestamp()
    public static final String SNAP_GETTIMESTAMP = "ac";
    // Snap.isVideo()
    public static final String SNAP_ISVIDEO = "al";

    // ReceivedSnap class
    public static final String RECEIVEDSNAP_CLASS = "com.snapchat.android.model.ReceivedSnap";
    // ReceivedSnap.getImageBitmap(Context)
    public static final String RECEIVEDSNAP_GETIMAGEBITMAP = "b";
    // ReceivedSnap.getSender()
    public static final String RECEIVEDSNAP_GETSENDER = "j";
    // ReceivedSnap.isScreenshotted()
    public static final String RECEIVEDSNAP_ISSCREENSHOTTED = "F";
    // ReceivedSnap.getCanonicalDisplayTime()
    public static final String RECEIVEDSNAP_DISPLAYTIME = "K";

    // StorySnap class
    public static final String STORYSNAP_CLASS = "com.snapchat.android.model.StorySnap";
    // StorySnap.getImageBitmap(Context)
    public static final String STORYSNAP_GETIMAGEBITMAP = "a";
    // StorySnap.getSender()
    public static final String STORYSNAP_GETSENDER = "aB";

    // ImageSnapRenderer Class
    public static final String IMAGESNAPRENDERER_CLASS = "com.snapchat.android.rendering.image.ImageSnapRenderer";
    // ImageSnapRenderer.start()
    public static final String IMAGESNAPRENDERER_START = "a";
    // ImageView instance in ImageSnapRenderer
    public static final String IMAGESNAPRENDERER_IMAGEVIEW = "h";
    // ReceivedSnap instance in ImageSnapRenderer
    public static final String IMAGESNAPRENDERER_RECEIVEDSNAP = "i";

    // VideoSnapRenderer Class
    public static final String VIDEOSNAPRENDERER_CLASS = "com.snapchat.android.rendering.video.VideoSnapRenderer";
    // VideoSnapRenderer.start()
    public static final String VIDEOSNAPRENDERER_START = "a";
    // VideoSnapRenderer.loadResources(VideoSnapResources)
    public static final String VIDEOSNAPRENDERER_LOADRES = "a";
    // ReceivedSnap instance in VideoSnapRenderer
    public static final String VIDEOSNAPRENDERER_RECEIVEDSNAP = "u";
    // SnapVideoView instance in VideoSnapRenderer
    public static final String VIDEOSNAPRENDERER_SNAPVIDEOVIEW = "o";

    // VideoSnapResources class
    public static final String VIDEOSNAPRESOURCES_CLASS = "com.snapchat.android.rendering.video.VideoSnapResources";
    // VideoSnapResources.getBitmap()
    public static final String VIDEOSNAPRESOURCES_GETBITMAP = "c";
    // VideoSnapResources.getUri()
    public static final String VIDEOSNAPRESOURCES_GETVIDEOURI = "b";

    // SnapListItemHandler class
    public static final String SNAPLISTITEMHANDLER_CLASS = "com.snapchat.android.util.SnapListItemHandler";
    // SnapListItemHandler.dispatchTouchEventSnap(MotionEvent, float, float, int)
    public static final String SNAPLISTITEMHANDLER_TOUCHEVENT_SNAP = "a";
    // SnapListItemHandler.dispatchTouchEventStory(MotionEvent, float, float, int)
    public static final String SNAPLISTITEMHANDLER_TOUCHEVENT_STORY = "b";
    // ImageView instance in SnapListItemHandler
    public static final String SNAPLISTITEMHANDLER_IMAGEVIEW = "d";

    // com.snapchat.android.ui.SnapView
    // SnapView.isViewing()
    public static final String SNAPVIEW_ISVIEWING = "b";

    // SnapPreviewFragment class
    public static final String SNAPPREVIEWFRAGMENT_CLASS = "com.snapchat.android.SnapPreviewFragment";
    // SnapPreviewFragment.prepareSnapForSending()
    public static final String SNAPPREVIEWFRAGMENT_PREPARESNAPFORSENDING = "k"; //prev. j
    // AnnotatedMediabryo instance variable name in SnapPreviewFragment
    public static final String SNAPPREVIEWFRAGMENT_VAR_MEDIABYRO = "x";

    // SnapImagebryo Class
    public static final String SNAPIMAGEBRYO_CLASS = "com.snapchat.android.model.SnapImagebryo";

    // com.snapchat.android.model.AnnotatedMediabryo
    // AnnotatedMediabryo.getImageBitmap()
    public static final String MEDIABRYO_GETSNAPBITMAP = "A"; //prev. w, x

    // com.snapchat.android.model.Mediabryo
    // Mediabryo.getVideoUri()
    public static final String MEDIABRYO_VIDEOURI = "H"; //prev. D,E

    // ImageResourceView Class
    public static final String IMAGERESOURCEVIEW_CLASS = "com.snapchat.android.ui.ImageResourceView";
    // ImageResource instance variable name
    public static final String IMAGERESOURCEVIEW_VAR_IMAGERESOURCE = "a";

    // com.snapchat.android.ui.ImageResource
    // ChatMedia instance variable name
    public static final String IMAGERESOURCE_VAR_CHATMEDIA = "d";

    // com.snapchat.android.model.chat.Chat
    // Chat.getTimestamp()
    public static final String CHAT_GETTIMESTAMP = "ac";

    // com.snapchat.android.model.chat.StatefulChatFeedItem
    // StatefulChatFeedItem.getSender()
    public static final String STATEFULCHATFEEDITEM_GETSENDER = "j";

    // ScreenshotDetector class
    public static final String SCREENSHOTDETECTOR_CLASS = "com.snapchat.android.screenshotdetection.ScreenshotDetector";
    // ScreenshotDetector.runDectectionSession()
    public static final String SCREENSHOTDETECTOR_RUNDECTECTIONSESSION = "a";
}
