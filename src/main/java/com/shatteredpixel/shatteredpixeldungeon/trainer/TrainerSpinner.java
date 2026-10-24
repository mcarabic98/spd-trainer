/*
 * Тренер для Shattered Pixel Dungeon v4.0.2 (SPD Trainer project).
 * Спиннер: подпись сверху, ниже значение в рамке с кнопками "<" ">".
 * Клик по значению открывает диалог ввода произвольного числа
 * (кнопки Set/Cancel, как в референс-меню модов).
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.shatteredpixel.shatteredpixeldungeon.trainer;

import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

public class TrainerSpinner extends Component {

	public interface OnChange {
		void onChange( int value );
	}

	private final String label;
	private final int min;
	private final int max;
	private int value;
	private OnChange listener;

	private RenderedTextBlock title;
	private RedButton btnMinus;
	private RedButton btnPlus;
	private ColorBlock valueBg;
	private BitmapText valText;
	private Button btnValue;

	public TrainerSpinner( String label, int min, int max, int value ) {
		super();
		this.label = label;
		this.min = min;
		this.max = max;
		this.value = Math.max( min, Math.min( max, value ) );
		//поля назначаются только ПОСЛЕ super(): createChildren() создаёт
		//пустые тексты, сюда подставляем реальные значения
		title.text( label );
		valText.text( String.valueOf( value ) );
	}

	@Override
	protected void createChildren() {
		title = PixelScene.renderTextBlock( "", 7 );
		add( title );

		btnMinus = new RedButton( "<" ) {
			@Override
			protected void onClick() {
				set( value - stepSize() );
			}
		};
		add( btnMinus );

		btnPlus = new RedButton( ">" ) {
			@Override
			protected void onClick() {
				set( value + stepSize() );
			}
		};
		add( btnPlus );

		valueBg = new ColorBlock( 1, 1, 0xFF444444 );
		add( valueBg );

		valText = new BitmapText( PixelScene.pixelFont );
		add( valText );

		btnValue = new Button() {
			@Override
			protected void onClick() {
				requestInput();
			}
		};
		add( btnValue );
	}

	private int stepSize() {
		long range = (long) max - min;
		if (range > 10_000_000) return 1_000_000;
		if (range > 100_000) return 1_000;
		if (range > 1_000) return 100;
		if (range > 100) return 10;
		return 1;
	}

	@Override
	protected void layout() {
		title.maxWidth( (int) width );
		title.setPos( x + (width - title.width()) / 2f, y );
		PixelScene.align( title );

		float rowY = y + title.height() + 1;
		float btnW = 16, btnH = 16;
		float valW = Math.max( 36, width - 2 * btnW - 8 );
		float totalW = 2 * btnW + valW + 8;

		btnMinus.setRect( x + (width - totalW) / 2f, rowY, btnW, btnH );
		valueBg.size( valW, btnH );
		valueBg.x = btnMinus.right() + 4;
		valueBg.y = rowY;
		valText.x = valueBg.x + (valW - valText.width()) / 2f;
		valText.y = rowY + (btnH - valText.baseLine()) / 2f;
		PixelScene.align( valText );
		btnValue.setRect( valueBg.x, rowY, valW, btnH );
		btnPlus.setRect( btnValue.right() + 4, rowY, btnW, btnH );

		height = btnPlus.bottom() - y;
	}

	public void set( int v ) {
		int clamped = Math.max( min, Math.min( max, v ) );
		if (clamped != value || !String.valueOf( clamped ).equals( valText.text() )) {
			value = clamped;
			valText.text( String.valueOf( value ) );
			layout();
			if (listener != null) {
				listener.onChange( value );
			}
		}
	}

	public int value() {
		return value;
	}

	public void setListener( OnChange listener ) {
		this.listener = listener;
	}

	/** Диалог произвольного значения (кнопки Set/Cancel) */
	private void requestInput() {
		GameScene.show( new WndTextInput( "Изменить значение",
				"Введите число от " + min + " до " + max,
				String.valueOf( value ), String.valueOf( max ).length(),
				false, "Set", "Cancel" ) {
			@Override
			public void onSelect( boolean positive, String text ) {
				if (!positive) {
					return;
				}
				try {
					set( Integer.parseInt( text.trim() ) );
				} catch (NumberFormatException e) {
					set( min );
				}
			}
		} );
	}
}
