package com.jfs.funkmachine2000;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * A custom Image View that also uses the chessboard output string to draw a
 * formatted chessboard on demand.
 * 
 * @author Floris, Jan
 * 
 */
public class ChessboardImageView extends ImageView {
	private Bitmap bmp;
	private String boardString;
	private Boolean formatted;
	public boolean initialized;

	public ChessboardImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.initialized = false;
	}

	/**
	 * Initialized some variables that couldn't be added in the constructor.
	 * 
	 * @param bitmap
	 *            The bitmap to show
	 * @param boardString
	 *            The string to use when drawing the chessboard
	 * @param formatted
	 *            The initial view setting
	 */
	public void init(Bitmap bitmap, String boardString, boolean formatted) {
		this.bmp = bitmap;
		this.boardString = boardString;
		this.formatted = formatted;
		// redraw view
		initialized = true;
		invalidate();
	}

	public boolean swap() {
		formatted = !formatted;
		// redraw view
		invalidate();
		return formatted;
	}

	@Override
	protected void onDraw(Canvas c) {
		super.onDraw(c);

		if (initialized) {
			if (formatted) {
				int cvWidth = this.getWidth();
				int cvHeight = this.getHeight();
				try {
					String[] board = boardString.split(":");
					String[] dim = board[0].split(",");
					String[] labels = board[2].split(",");
					int nsquaresx = Integer.parseInt(dim[0]);
					int nsquaresy = Integer.parseInt(dim[1]);

					// Square width/height
					float w = cvWidth / nsquaresx;
					float h = cvHeight / nsquaresy;

					// Initialize Paint
					Paint dark = new Paint();
					TextPaint darkText = new TextPaint();
					dark.setARGB(255, 0, 0, 0);
					darkText.setARGB(255, 0, 0, 0);
					darkText.setTextSize(12);
					Paint light = new Paint();
					TextPaint lightText = new TextPaint();
					light.setARGB(255, 255, 255, 255);
					lightText.setARGB(255, 255, 255, 255);
					lightText.setTextSize(12);

					int col, row;
					// Loop through squares
					for (col = 0; col < nsquaresy; col++) {
						for (row = 0; row < nsquaresx; row++) {
							// Determine if square is light or dark
							StaticLayout layout;
							if ((col % 2 == 0 && row % 2 == 0)
									|| (col % 2 == 1 && row % 2 == 1)) {
								c.drawRect(col * w, row * h, (col + 1) * w,
										(row + 1) * h, light);
								layout = new StaticLayout(labels[col
										* nsquaresy + row], darkText,
										(int) w - 10,
										Layout.Alignment.ALIGN_NORMAL, 1.3f, 0,
										false);
							} else {
								c.drawRect(col * w, row * h, (col + 1) * w,
										(row + 1) * h, dark);
								// Create a StaticLayout so the text is wrapped
								// properly inside the square
								layout = new StaticLayout(labels[col
										* nsquaresy + row], lightText,
										(int) w - 10,
										Layout.Alignment.ALIGN_NORMAL, 1.3f, 0,
										false);
							}
							c.save();
							c.translate(col * w + 5, row * h + 5); // position
																	// the text
							layout.draw(c);
							c.restore();
						}
					}
				} catch (Exception e) {
					System.out.println("Invalid string.");
				}
			} else {
				this.setImageBitmap(bmp);
			}
		}
	}
}
