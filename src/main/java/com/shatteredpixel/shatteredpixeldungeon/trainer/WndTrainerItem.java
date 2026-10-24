/*
 * Тренер для Shattered Pixel Dungeon v4.0.2 (SPD Trainer project).
 * Редактор предмета: иконка+название, описание, спиннер количества;
 * для улучшаемых — уровень улучшения и тумблеры; для жезлов — заряды;
 * для оружия — зачарования/проклятия; для брони — глифы/проклятия.
 * Кнопки «Дать» и «Назад».
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.shatteredpixel.shatteredpixeldungeon.trainer;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

public class WndTrainerItem extends Window {

	private static final int WIDTH	= 160;
	private static final int BTN_H	= 18;
	private static final int GAP	= 2;
	private static final int MAX_BODY_H = 220;

	/** Зачарования оружия (без проклятий) */
	private static final Class<?>[] WEAPON_ENCHANTS = {
			Blazing.class, Blocking.class, Blooming.class, Chilling.class,
			Corrupting.class, Crystal.class, Elastic.class, Grim.class,
			Kinetic.class, Lucky.class, Projecting.class, Shocking.class,
			Unstable.class, Vampiric.class, Venomous.class, Vorpal.class
	};

	/** Проклятия оружия */
	private static final Class<?>[] WEAPON_CURSES = {
			Annoying.class, Dazzling.class, Displacing.class, Explosive.class,
			Friendly.class, Polarized.class, Pressurized.class, Sacrificial.class,
			Wayward.class, Wondrous.class
	};

	/** Глифы брони (без проклятий) */
	private static final Class<?>[] ARMOR_GLYPHS = {
			Affection.class, AntiMagic.class, Brimstone.class, Camouflage.class,
			Entanglement.class, Flow.class, Obfuscation.class, Potential.class,
			Repulsion.class, Stone.class, Swiftness.class, Thorns.class,
			Viscosity.class
	};

	/** Проклятия брони */
	private static final Class<?>[] ARMOR_CURSES = {
			AntiEntropy.class, Bulk.class, Corrosion.class, Displacement.class,
			Metabolism.class, Multiplicity.class, Overgrowth.class, Stench.class
	};

	private WndTrainer.RefreshableScrollPane pane;

	public WndTrainerItem( final Item item ) {
		super();
		Trainer.log( "item editor: " + item.name() + " x" + item.quantity() );

		//--- шапка: иконка + название
		IconTitle head = new IconTitle( item );
		head.setRect( 0, 0, WIDTH, 0 );
		add( head );
		float pos = head.bottom() + 2;

		//--- тело в прокрутке
		//панель создаётся ДО наполнения (иначе контроллер прокрутки
		//глотает клики всех элементов внутри)
		Component body = new Component();
		pane = new WndTrainer.RefreshableScrollPane( body );
		add( pane );
		float bpos = 0;

		RenderedTextBlock desc = PixelScene.renderTextBlock( item.desc(), 6 );
		desc.maxWidth( WIDTH - 6 );
		desc.setPos( 3, bpos );
		body.add( desc );
		bpos = desc.bottom() + 4;

		final TrainerSpinner spQty = new TrainerSpinner(
				"Количество", 1, 2_000_000_000, item.quantity() );
		spQty.setRect( 0, bpos, WIDTH, 0 );
		body.add( spQty );
		bpos = spQty.bottom() + 4;

		final Wand wand = item instanceof Wand ? (Wand) item : null;
		TrainerSpinner spCharge = null;
		TrainerSpinner spLevel = null;
		CheckBox cbCursed = null;
		CheckBox cbCursedKnown = null;
		CheckBox cbIdentified = null;

		if (wand != null) {
			spCharge = new TrainerSpinner( "Заряды", 0, 2_000_000_000, wand.curCharges );
			spCharge.setRect( 0, bpos, WIDTH, 0 );
			body.add( spCharge );
			bpos = spCharge.bottom() + 4;
		}

		//уровень: у всего, что улучшается, плюс артефакты и безделушки
		//(они не «upgradable» по флагу, но уровень имеют)
		if (item.isUpgradable() || item instanceof Artifact || item instanceof Trinket) {
			final Item fItem = item;

			spLevel = new TrainerSpinner( "Уровень улучшения",
					-2_000_000_000, 2_000_000_000, item.level() );
			spLevel.setRect( 0, bpos, WIDTH, 0 );
			body.add( spLevel );
			bpos = spLevel.bottom() + 4;

			cbCursed = new CheckBox( "Проклято" ) {
				@Override
				protected void onClick() {
					super.onClick();
					fItem.cursed = checked();
					Trainer.log( "cursed = " + checked() );
				}
			};
			cbCursed.checked( item.cursed );
			cbCursed.setRect( 0, bpos, WIDTH, BTN_H );
			body.add( cbCursed );
			bpos += BTN_H + GAP;

			cbCursedKnown = new CheckBox( "Проклятие известно" ) {
				@Override
				protected void onClick() {
					super.onClick();
					fItem.cursedKnown = checked();
					Trainer.log( "cursedKnown = " + checked() );
				}
			};
			cbCursedKnown.checked( item.cursedKnown );
			cbCursedKnown.setRect( 0, bpos, WIDTH, BTN_H );
			body.add( cbCursedKnown );
			bpos += BTN_H + GAP;

			cbIdentified = new CheckBox( "Уровень известен" ) {
				@Override
				protected void onClick() {
					super.onClick();
					fItem.levelKnown = checked();
					Trainer.log( "levelKnown = " + checked() );
				}
			};
			cbIdentified.checked( item.levelKnown );
			cbIdentified.setRect( 0, bpos, WIDTH, BTN_H );
			body.add( cbIdentified );
			bpos += BTN_H + GAP;
		}

		if (item instanceof Weapon) {
			bpos = addModifierSection( body, bpos, "Зачарования",
					(Weapon) item, WEAPON_ENCHANTS );
			bpos = addModifierSection( body, bpos, "Проклятия оружия",
					(Weapon) item, WEAPON_CURSES );
		}
		if (item instanceof Armor) {
			bpos = addGlyphSection( body, bpos, "Глифы", (Armor) item, ARMOR_GLYPHS );
			bpos = addGlyphSection( body, bpos, "Проклятия брони", (Armor) item, ARMOR_CURSES );
		}

		body.setSize( WIDTH - 2, bpos );

		float maxBody = Math.min( MAX_BODY_H,
				PixelScene.uiCamera.height - head.height() - BTN_H - 20 );
		float bodyH = Math.max( 40, Math.min( bpos, maxBody ) );
		pane.setRect( 1, pos, WIDTH - 2, bodyH );
		pane.scrollTo( 0, 0 );
		pos += bodyH + 4;

		//--- нижние кнопки
		final TrainerSpinner fCharge = spCharge;
		final TrainerSpinner fLevel = spLevel;
		final CheckBox fCursed = cbCursed;
		final CheckBox fCursedKnown = cbCursedKnown;
		final CheckBox fIdentified = cbIdentified;

		RedButton btnGive = new RedButton( "Дать" ) {
			@Override
			protected void onClick() {
				apply( item, wand, spQty, fCharge, fLevel, fCursed, fCursedKnown, fIdentified );
				Trainer.giveItem( item );
				hide();
			}
		};
		add( btnGive );
		btnGive.setRect( 0, pos, (WIDTH - GAP) / 2f, BTN_H );

		RedButton btnBack = new RedButton( "Назад" ) {
			@Override
			protected void onClick() {
				hide();
				WndTrainer.lastTab = WndTrainer.TAB_ITEMS;
				GameScene.show( new WndTrainer() );
			}
		};
		add( btnBack );
		btnBack.setRect( (WIDTH - GAP) / 2f + GAP, pos, (WIDTH - GAP) / 2f, BTN_H );
		pos += BTN_H;

		resize( WIDTH, (int) pos );
	}

	@Override
	public void offset( int xOffset, int yOffset ) {
		//намеренно игнорируем наследуемый сдвиг (см. WndTrainer):
		//окно всегда центрируется, контент прокрутки не уезжает
	}

	/** Секция зачарований/проклятий оружия: сетка 2 колонки + «снять» */
	private float addModifierSection( Component body, float bpos, String title,
	                                  final Weapon weapon, Class<?>[] classes ) {
		bpos = addHeader( body, bpos, title );
		float bw = (WIDTH - GAP) / 2f;
		int i = 0;
		for (final Class<?> cls : classes) {
			RedButton btn = new RedButton( modifierName( cls ), 7 ) {
				@Override
				protected void onClick() {
					weapon.enchant( (Weapon.Enchantment) Reflection.newInstance( cls ) );
					Trainer.log( "enchant: " + weapon.name() + " <- " + cls.getSimpleName() );
				}
			};
			if (weapon.enchantment != null && weapon.enchantment.getClass() == cls) {
				btn.textColor( Window.TITLE_COLOR );
			}
			body.add( btn );
			btn.setRect( (i % 2) * (bw + GAP), bpos, bw, BTN_H );
			if (i % 2 == 1) {
				bpos += BTN_H + GAP;
			}
			i++;
		}
		if (i % 2 == 1) {
			bpos += BTN_H + GAP;
		}
		bpos = fullButton( body, bpos, "Снять зачарование", new Runnable() {
			@Override
			public void run() {
				weapon.enchant( null );
				Trainer.log( "enchant removed" );
			}
		} );
		return bpos;
	}

	/** Секция глифов/проклятий брони: сетка 2 колонки + «снять» */
	private float addGlyphSection( Component body, float bpos, String title,
	                               final Armor armor, Class<?>[] classes ) {
		bpos = addHeader( body, bpos, title );
		float bw = (WIDTH - GAP) / 2f;
		int i = 0;
		for (final Class<?> cls : classes) {
			RedButton btn = new RedButton( modifierName( cls ), 7 ) {
				@Override
				protected void onClick() {
					armor.inscribe( (Armor.Glyph) Reflection.newInstance( cls ) );
					Trainer.log( "glyph: " + armor.name() + " <- " + cls.getSimpleName() );
				}
			};
			if (armor.glyph != null && armor.glyph.getClass() == cls) {
				btn.textColor( Window.TITLE_COLOR );
			}
			body.add( btn );
			btn.setRect( (i % 2) * (bw + GAP), bpos, bw, BTN_H );
			if (i % 2 == 1) {
				bpos += BTN_H + GAP;
			}
			i++;
		}
		if (i % 2 == 1) {
			bpos += BTN_H + GAP;
		}
		bpos = fullButton( body, bpos, "Снять глиф", new Runnable() {
			@Override
			public void run() {
				armor.inscribe( null );
				Trainer.log( "glyph removed" );
			}
		} );
		return bpos;
	}

	private float addHeader( Component body, float bpos, String title ) {
		RenderedTextBlock head = PixelScene.renderTextBlock( title, 8 );
		head.hardlight( Window.TITLE_COLOR );
		head.setPos( 3, bpos + 1 );
		body.add( head );
		return bpos + Math.max( 12, head.height() + 3 );
	}

	private float fullButton( Component body, float bpos, String label, final Runnable action ) {
		RedButton btn = new RedButton( label ) {
			@Override
			protected void onClick() {
				action.run();
			}
		};
		body.add( btn );
		btn.setRect( 0, bpos, WIDTH, BTN_H );
		return bpos + BTN_H + GAP;
	}

	/** Локализованное имя зачарования/глифа по классу */
	private static String modifierName( Class<?> cls ) {
		try {
			Object mod = Reflection.newInstance( cls );
			java.lang.reflect.Method name = mod.getClass().getMethod( "name" );
			return String.valueOf( name.invoke( mod ) );
		} catch (Exception e) {
			return cls.getSimpleName();
		}
	}

	private void apply( Item item, Wand wand,
	                    TrainerSpinner spQty, TrainerSpinner spCharge, TrainerSpinner spLevel,
	                    CheckBox cbCursed, CheckBox cbCursedKnown, CheckBox cbIdentified ) {
		item.quantity( spQty.value() );
		if (spLevel != null) {
			item.level( spLevel.value() );
		}
		if (cbCursed != null) {
			item.cursed = cbCursed.checked();
		}
		if (cbCursedKnown != null) {
			item.cursedKnown = cbCursedKnown.checked();
		}
		if (cbIdentified != null) {
			item.levelKnown = cbIdentified.checked();
		}
		if (wand != null && spCharge != null) {
			//без кэпа к maxCharges: позволяет «бесконечные» жезлы
			wand.curCharges = spCharge.value();
		}
		Trainer.log( "item applied: " + item.name()
				+ " x" + item.quantity()
				+ " lvl" + item.level() );
	}
}
