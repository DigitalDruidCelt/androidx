package androidx.car.app.aaos.renderer.surface;

import androidx.car.app.aaos.renderer.surface.SurfaceWrapper;

/**
 * Interface implemented by an off-process renderer to receive events affecting the
 * {@link SurfaceView} it renders content on.
 *
 * @hide
 */
interface ISurfaceControl {
  /** Notifies that the underlying surface changed. */
  void setSurfaceWrapper(in SurfaceWrapper surfaceWrapper) = 1;

  /** Notifies that the surface received a new touch event. */
  void onTouchEvent(in MotionEvent event) = 2;

  /** Notifies that the window focus changed. */
  void onWindowFocusChanged(boolean hasFocus, boolean isInTouchMode) = 3;
}
