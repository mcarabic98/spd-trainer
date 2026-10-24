/*
 * Тренер для Shattered Pixel Dungeon v4.0.2 (SPD Trainer project).
 * Окно инспекции клетки: отладочная сводка от Trainer.cellReport().
 * Закрывается кликом вне окна или ESC.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.shatteredpixel.shatteredpixeldungeon.trainer;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

public class WndInspectCell extends Window {

	private static final int WIDTH = 158;

	public WndInspectCell( String report ) {
		super();

		RenderedTextBlock text = PixelScene.renderTextBlock( report, 7 );
		text.maxWidth( WIDTH - 8 );
		add( text );
		text.setPos( 4, 4 );

		resize( WIDTH, (int) (text.height() + 8) );
	}

	@Override
	public void offset( int xOffset, int yOffset ) {
		//намеренно игнорируем наследуемый сдвиг (см. WndTrainer)
	}
}
