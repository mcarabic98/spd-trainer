/*
 * Тренер для Shattered Pixel Dungeon v4.0.2 (SPD Trainer project).
 * Главное окно трейнера: вкладки внизу, контент в прокрутке.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.shatteredpixel.shatteredpixeldungeon.trainer;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.*;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.*;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GuardianTrap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.GameMath;
import com.watabou.utils.Point;
import com.watabou.utils.Reflection;

public class WndTrainer extends Window {

	private static final int TAB_CHEATS = 0;
	public static final int TAB_ITEMS = 1;
	public static final int TAB_HERO = 2;
	public static final int TAB_BUFFS = 3;
	public static final int TAB_MOBS = 4;
	public static final int TAB_DUNGEON = 5;
	public static final int TAB_INFO = 6;
	private static final int TAB_COUNT = 7;

	private static final String[] TAB_NAMES =
			{ "Читы", "Предм.", "Герой", "Баффы", "Мобы", "Данж", "Инфо" };

	// ==================== Предметы ====================

	private static final String[] CAT_NAMES = {
			"Оружие", "Метат.", "Броня", "Жезлы",
			"Кольца", "Артефакты", "Зелья", "Свитки",
			"Руны", "Семена", "Еда", "Безделушки",
			"Экз. зелья", "Экз. свитки", "Бомбы",
			"Ключи", "Квестовые", "Прочее"
	};

	private static final Generator.Category[][] CATEGORIES = {
			{ Generator.Category.WEP_T1, Generator.Category.WEP_T2, Generator.Category.WEP_T3,
					Generator.Category.WEP_T4, Generator.Category.WEP_T5 },
			{ Generator.Category.MIS_T1, Generator.Category.MIS_T2, Generator.Category.MIS_T3,
					Generator.Category.MIS_T4, Generator.Category.MIS_T5 },
			{ Generator.Category.ARMOR },
			{ Generator.Category.WAND },
			{ Generator.Category.RING },
			{ Generator.Category.ARTIFACT },
			{ Generator.Category.POTION },
			{ Generator.Category.SCROLL },
			{ Generator.Category.STONE },
			{ Generator.Category.SEED },
			{ Generator.Category.FOOD },
			{ Generator.Category.TRINKET },
			//остальные категории — только списками EXTRAS
			new Generator.Category[0], new Generator.Category[0], new Generator.Category[0],
			new Generator.Category[0], new Generator.Category[0], new Generator.Category[0]
	};

	/** Предметы вне списков генератора, по индексу категории */
	private static final Class<?>[][] EXTRAS = new Class<?>[CATEGORIES.length][];

	static {
		EXTRAS[5] = new Class<?>[]{ // «Артефакты» — вне генератора
				CapeOfThorns.class,
				LloydsBeacon.class
		};
		EXTRAS[10] = new Class<?>[]{ // «Еда»
				ChargrilledMeat.class, FrozenCarpaccio.class, StewedMeat.class,
				MeatPie.class, SmallRation.class, SupplyRation.class,
				Blandfruit.class, Berry.class, PhantomMeat.class
		};
		EXTRAS[11] = new Class<?>[]{ // «Безделушки»
				TrinketCatalyst.class
		};
		EXTRAS[12] = new Class<?>[]{ // «Экз. зелья»
				PotionOfCleansing.class, PotionOfCorrosiveGas.class, PotionOfDivineInspiration.class,
				PotionOfDragonsBreath.class, PotionOfEarthenArmor.class, PotionOfMagicalSight.class,
				PotionOfMastery.class, PotionOfShielding.class, PotionOfShroudingFog.class,
				PotionOfSnapFreeze.class, PotionOfStamina.class, PotionOfStormClouds.class
		};
		EXTRAS[13] = new Class<?>[]{ // «Экз. свитки»
				ScrollOfAntiMagic.class, ScrollOfChallenge.class, ScrollOfDivination.class,
				ScrollOfDread.class, ScrollOfEnchantment.class, ScrollOfForesight.class,
				ScrollOfMetamorphosis.class, ScrollOfMysticalEnergy.class, ScrollOfPassage.class,
				ScrollOfPrismaticImage.class, ScrollOfPsionicBlast.class, ScrollOfSirensSong.class
		};
		EXTRAS[14] = new Class<?>[]{ // «Бомбы»
				Bomb.class, Firebomb.class, FrostBomb.class, FlashBangBomb.class,
				ShrapnelBomb.class, SmokeBomb.class, RegrowthBomb.class, HolyBomb.class,
				ArcaneBomb.class, Noisemaker.class, WoollyBomb.class
		};
		EXTRAS[15] = new Class<?>[]{ // «Ключи»
				IronKey.class, GoldenKey.class, CrystalKey.class, WornKey.class
		};
		EXTRAS[16] = new Class<?>[]{ // «Квестовые»
				CeremonialCandle.class, CorpseDust.class, DarkGold.class, DwarfToken.class,
				Embers.class, EscapeCrystal.class, GooBlob.class, ImpStatue.class,
				MetalShard.class, Pickaxe.class, VaultBeacon.class
		};
		EXTRAS[17] = new Class<?>[]{ // «Прочее»
				Amulet.class, Ankh.class, ArcaneResin.class, BrokenSeal.class,
				EnergyCrystal.class, Honeypot.class, KingsCrown.class, LiquidMetal.class,
				LostBackpack.class, Stylus.class, TengusMask.class, Torch.class, Waterskin.class
		};
	}

	// ==================== Баффы ====================

	private static final String[] BUFF_SECTION_NAMES = {
			"Положительные", "Отрицательные", "Механики классов", "На ближайшего моба"
	};

	private static final Class<?>[][] BUFF_SECTIONS = {
			{ // Положительные
					Adrenaline.class, AdrenalineSurge.class, ArcaneArmor.class, Awareness.class,
					Barkskin.class, Barrier.class, Bless.class, FireImbue.class,
					Foresight.class, FrostImbue.class, GreaterHaste.class, Haste.class,
					Healing.class, HeroDisguise.class, Invisibility.class, Invulnerability.class,
					Levitation.class, Light.class, MagicImmune.class, MagicalSight.class,
					MindVision.class, PrismaticGuard.class, Recharging.class, Regeneration.class,
					Shadows.class, Speed.class, Stamina.class,
					TimeStasis.class, ToxicImbue.class, WellFed.class
			},
			{ // Отрицательные
					Amok.class, Bleeding.class, Blindness.class, Burning.class,
					Charm.class, Chill.class, Corrosion.class, Cripple.class,
					Daze.class, Degrade.class, Doom.class, Dread.class,
					Drowsy.class, Frost.class, Hex.class, MagicalSleep.class,
					Ooze.class, Paralysis.class, Poison.class, Roots.class,
					Sleep.class, Slow.class, Terror.class, Vertigo.class,
					Vulnerable.class, Weakness.class
			},
			{ // Механики классов
					Berserk.class, Combo.class, EnhancedRings.class, Fury.class,
					HoldFast.class, MonkEnergy.class, Momentum.class, PhysicalEmpower.class,
					Preparation.class, RevealedArea.class, ScrollEmpower.class, WandEmpower.class
			},
			{ // На ближайшего моба (SoulMark/SnipersMark — метки снайпера, врагу)
					Amok.class, Bleeding.class, Blindness.class, Burning.class,
					Charm.class, Chill.class, Corrosion.class, Cripple.class,
					Daze.class, Doom.class, Dread.class, Drowsy.class,
					Frost.class, Hex.class, MagicalSleep.class, Ooze.class,
					Paralysis.class, Poison.class, Roots.class, Sleep.class,
					Slow.class, SnipersMark.class, SoulMark.class, Terror.class,
					Vertigo.class, Vulnerable.class, Weakness.class
			}
	};

	// ==================== Мобы: секции по бестиарию игры ====================

	private static final String[] MOB_SECTION_NAMES = {
			"Канализация", "Тюрьма", "Пещеры", "Город", "Демонические залы",
			"Боссы", "Универсальные", "Редкие", "Квестовые", "NPC", "Союзники"
	};

	private static final Class<?>[][] MOB_SECTIONS = {
			{ // Канализация
					Rat.class, Snake.class, Gnoll.class, Swarm.class, Crab.class, Slime.class
			},
			{ // Тюрьма
					Skeleton.class, Thief.class, DM100.class, Guard.class, Necromancer.class
			},
			{ // Пещеры
					Bat.class, Brute.class, Spinner.class, DM200.class,
					Shaman.RedShaman.class, Shaman.BlueShaman.class, Shaman.PurpleShaman.class
			},
			{ // Город
					Ghoul.class, Warlock.class, Monk.class, Golem.class,
					Elemental.FireElemental.class, Elemental.FrostElemental.class, Elemental.ShockElemental.class
			},
			{ // Демонические залы
					RipperDemon.class, DemonSpawner.class, Succubus.class, Eye.class, Scorpio.class
			},
			{ // Боссы
					Goo.class, Tengu.class, Pylon.class, DM300.class, DwarfKing.class,
					YogDzewa.Larva.class, YogDzewa.class,
					YogFist.BurningFist.class, YogFist.SoiledFist.class, YogFist.RottingFist.class,
					YogFist.RustedFist.class, YogFist.BrightFist.class, YogFist.DarkFist.class
			},
			{ // Универсальные
					Wraith.class, Piranha.class, Mimic.class, GoldenMimic.class, EbonyMimic.class,
					Statue.class, GuardianTrap.Guardian.class, SentryRoom.Sentry.class
			},
			{ // Редкие
					Albino.class, CausticSlime.class, Bandit.class, SpectralNecromancer.class,
					ArmoredBrute.class, DM201.class, Elemental.ChaosElemental.class, Senior.class,
					Acidic.class, TormentedSpirit.class, PhantomPiranha.class,
					CrystalMimic.class, ArmoredStatue.class, GnollExile.class, HermitCrab.class
			},
			{ // Квестовые
					FetidRat.class, GnollTrickster.class, GreatCrab.class,
					Elemental.NewbornFireElemental.class, RotLasher.class, RotHeart.class,
					CrystalWisp.class, CrystalGuardian.class, CrystalSpire.class,
					GnollGuard.class, GnollSapper.class, GnollGeomancer.class,
					VaultSkeleton.class, VaultDM100.class, VaultShaman.class, VaultDM200.class,
					VaultSentry.class, VaultLaser.class, VaultBossElemental.class
			},
			{ // NPC
					Ghost.class, RatKing.class, Shopkeeper.class, Wandmaker.class,
					Blacksmith.class, Imp.class, Sheep.class, Bee.class
			},
			{ // Союзники (спавнятся как ALLY принудительно)
					MirrorImage.class, PrismaticImage.class, DriedRose.GhostHero.class,
					WandOfWarding.Ward.class, WandOfWarding.Ward.WardSentry.class,
					WandOfLivingEarth.EarthGuardian.class, ShadowClone.ShadowAlly.class,
					SmokeBomb.NinjaLog.class, SpiritHawk.HawkAlly.class, PowerOfMany.LightAlly.class
			}
	};

	private static final int BTN_H = 18;
	private static final int GAP = 2;
	private static final int CELL = 24;
	private static final int CATS_PER_ROW = 4;
	private static final int MAX_INT = 2_000_000_000;

	/** Последняя открытая вкладка и категория (помнятся между открытиями) */
	public static int lastTab = TAB_CHEATS;
	public static int lastCat = 0;

	private final RefreshableScrollPane pane;

	public WndTrainer() {
		super();

		//окно должно влезать в UI-камеру при любом масштабе
		int w = (int) Math.min( 260, PixelScene.uiCamera.width - 8 );
		int h = (int) Math.min( 240, PixelScene.uiCamera.height - 8 );

		Trainer.log( "trainer menu opened: tab=" + lastTab + " cat=" + lastCat
				+ " win=" + w + "x" + h );

		//панель создаётся ДО кнопок: контроллер прокрутки регистрируется
		//в стеке событий позже и глотает клики элементов, созданных раньше
		Component content = new Component();
		pane = new RefreshableScrollPane( content );
		add( pane );

		float pos;
		switch (lastTab) {
			case TAB_ITEMS: pos = buildItemsTab( content, w ); break;
			case TAB_HERO: pos = buildHeroTab( content, w ); break;
			case TAB_BUFFS: pos = buildBuffsTab( content, w ); break;
			case TAB_MOBS: pos = buildMobsTab( content, w ); break;
			case TAB_DUNGEON: pos = buildDungeonTab( content, w ); break;
			case TAB_INFO: pos = buildInfoTab( content, w ); break;
			case TAB_CHEATS:
			default: pos = buildCheatsTab( content, w ); break;
		}
		content.setSize( w, pos );

		float paneH = Math.min( pos, h - BTN_H - GAP * 2 );
		pane.setRect( 0, 0, w, paneH );
		pane.scrollTo( 0, 0 );

		//панель вкладок внизу
		float tabW = (w - (TAB_COUNT - 1) * GAP) / TAB_COUNT;
		for (int i = 0; i < TAB_COUNT; i++) {
			final int tabId = i;
			RedButton tab = new RedButton( TAB_NAMES[i], 7 ) {
				@Override
				protected void onClick() {
					if (lastTab != tabId) {
						lastTab = tabId;
						hide();
						GameScene.show( new WndTrainer() );
					}
				}
			};
			if (lastTab == tabId) {
				tab.textColor( Window.TITLE_COLOR );
			}
			add( tab );
			tab.setRect( i * (tabW + GAP), h - BTN_H, tabW, BTN_H );
		}

		resize( w, h );
	}

	@Override
	public void offset( int xOffset, int yOffset ) {
		//намеренно игнорируем наследуемый сдвиг предыдущих окон:
		//сдвиг камеры окна рассинхронизирует камеру прокрутки.
	}

	// ==================== Вкладка «Читы» ====================

	private float buildCheatsTab( Component content, float w ) {
		float pos = 0;

		CheckBox godBox = new CheckBox( "Режим бога" ) {
			@Override
			protected void onClick() {
				super.onClick();
				Trainer.godMode = checked();
				Trainer.log( "god mode " + (Trainer.godMode ? "ON" : "OFF") );
			}
		};
		godBox.checked( Trainer.godMode );
		godBox.setRect( 0, pos, w, BTN_H );
		content.add( godBox );
		pos += BTN_H + GAP;

		pos = cheatRow( content, pos, w, "Полное здоровье", Trainer::fullHealth );
		pos = cheatRow( content, pos, w, "+1000 золота", () -> Trainer.addGold( 1000 ) );
		pos = cheatRow( content, pos, w, "+1 уровень", Trainer::levelUp );
		pos = cheatRow( content, pos, w, "+1 сила", Trainer::addStrength );
		pos = cheatRow( content, pos, w, "Открыть карту", Trainer::revealMap );
		pos = cheatRow( content, pos, w, "Убрать голод", Trainer::removeHunger );
		pos = cheatRow( content, pos, w, "Перезарядить жезлы", Trainer::chargeWands );
		return pos;
	}

	// ==================== Вкладка «Предметы» ====================

	private float buildItemsTab( Component content, float w ) {
		float pos = 0;

		//категории: 4 кнопки в ряд
		float bw = (w - (CATS_PER_ROW - 1) * GAP) / CATS_PER_ROW;
		for (int i = 0; i < CAT_NAMES.length; i++) {
			final int idx = i;
			RedButton catBtn = new RedButton( CAT_NAMES[i], 7 ) {
				@Override
				protected void onClick() {
					if (lastCat != idx) {
						lastCat = idx;
						hide(); //иначе окна накапливаются и ESC приходится жать много раз
						GameScene.show( new WndTrainer() );
					}
				}
			};
			if (lastCat == idx) {
				catBtn.textColor( Window.TITLE_COLOR );
			}
			content.add( catBtn );
			catBtn.setRect( (i % CATS_PER_ROW) * (bw + GAP), pos, bw, BTN_H );
			if (i % CATS_PER_ROW == CATS_PER_ROW - 1 || i == CAT_NAMES.length - 1) {
				pos += BTN_H + GAP;
			}
		}
		pos += GAP;

		//иконки предметов выбранной категории (+ дополнения вне генератора)
		int iconCols = Math.max( 4, (int) ((w - 4) / CELL) );
		int col = 0;
		for (Generator.Category cat : CATEGORIES[lastCat]) {
			if (cat.classes == null) {
				continue;
			}
			for (Class<?> cls : cat.classes) {
				Item item = newItem( cls );
				if (item == null) {
					continue;
				}
				ItemIconButton btn = new ItemIconButton( item );
				btn.setRect( 2 + col * CELL, pos, CELL - 2, CELL - 2 );
				content.add( btn );
				col++;
				if (col == iconCols) {
					col = 0;
					pos += CELL;
				}
			}
		}
		if (EXTRAS[lastCat] != null) {
			for (Class<?> cls : EXTRAS[lastCat]) {
				Item item = newItem( cls );
				if (item == null) {
					continue;
				}
				ItemIconButton btn = new ItemIconButton( item );
				btn.setRect( 2 + col * CELL, pos, CELL - 2, CELL - 2 );
				content.add( btn );
				col++;
				if (col == iconCols) {
					col = 0;
					pos += CELL;
				}
			}
		}
		if (col > 0) {
			pos += CELL;
		}
		return pos + 2;
	}

	// ==================== Вкладка «Герой» ====================

	private float buildHeroTab( Component content, float w ) {
		final Hero hero = Dungeon.hero;
		float pos = 0;

		TrainerSpinner spHP = new TrainerSpinner( "Здоровье", 1, MAX_INT, hero.HP );
		spHP.setListener( Trainer::setHP );
		spHP.setRect( 0, pos, w, 0 );
		content.add( spHP );
		pos = spHP.bottom() + 4;

		TrainerSpinner spHT = new TrainerSpinner( "Макс. здоровье", 1, MAX_INT, hero.HT );
		spHT.setListener( Trainer::setHT );
		spHT.setRect( 0, pos, w, 0 );
		content.add( spHT );
		pos = spHT.bottom() + 4;

		TrainerSpinner spLevel = new TrainerSpinner( "Уровень (кап игры: 30)", 1, 30, hero.lvl );
		spLevel.setListener( Trainer::setLevel );
		spLevel.setRect( 0, pos, w, 0 );
		content.add( spLevel );
		pos = spLevel.bottom() + 4;

		TrainerSpinner spSTR = new TrainerSpinner( "Сила", 1, MAX_INT, hero.STR );
		spSTR.setListener( Trainer::setSTR );
		spSTR.setRect( 0, pos, w, 0 );
		content.add( spSTR );
		pos = spSTR.bottom() + 4;

		TrainerSpinner spGold = new TrainerSpinner( "Золото", 0, MAX_INT, Dungeon.gold );
		spGold.setListener( Trainer::setGold );
		spGold.setRect( 0, pos, w, 0 );
		content.add( spGold );
		pos = spGold.bottom() + 6;

		pos = cheatRow( content, pos, w, "Голод: сыт (0)", () -> Trainer.setHungerStage( 0 ) );
		pos = cheatRow( content, pos, w, "Голод: голоден (300)", () -> Trainer.setHungerStage( 300 ) );
		pos = cheatRow( content, pos, w, "Голод: голодает (450)", () -> Trainer.setHungerStage( 450 ) );

		pos = cheatRow( content, pos, w, "Сбросить таланты", Trainer::resetTalents );
		return pos;
	}

	// ==================== Вкладка «Баффы» ====================

	private float buildBuffsTab( Component content, float w ) {
		float pos = 0;

		final TrainerSpinner spDur = new TrainerSpinner( "Значение (длит./кол-во)", 1, MAX_INT, 10 );
		spDur.setRect( 0, pos, w, 0 );
		content.add( spDur );
		pos = spDur.bottom() + 4;

		for (int s = 0; s < BUFF_SECTION_NAMES.length; s++) {
			pos = addTabHeader( content, pos, BUFF_SECTION_NAMES[s] );
			//последняя секция — баффы на ближайшего моба
			boolean onMob = (s == BUFF_SECTION_NAMES.length - 1);
			pos = addBuffGrid( content, pos, w, BUFF_SECTIONS[s], spDur, onMob );
		}

		pos = cheatRow( content, pos, w, "Снять все баффы", Trainer::removeAllBuffs );
		return pos;
	}

	private float addBuffGrid( Component content, float pos, float w,
	                           Class<?>[] classes, final TrainerSpinner spDur, final boolean onMob ) {
		float bw = (w - GAP) / 2f;
		int i = 0;
		for (final Class<?> c : classes) {
			final Class<? extends Buff> bCls = c.asSubclass( Buff.class );

			String name = buffName( bCls );
			Image icon = null;
			try {
				Buff b = (Buff) Reflection.newInstance( bCls );
				icon = new BuffIcon( b, false );
			} catch (Exception ignored) {
			}

			RedButton btn = new RedButton( name, 7 ) {
				@Override
				protected void onClick() {
					if (onMob) {
						Trainer.applyBuffToNearestMob( bCls, spDur.value() );
					} else {
						Trainer.applyBuff( Trainer.hero(), bCls, spDur.value() );
					}
				}
			};
			if (icon != null) {
				btn.icon( icon );
			}
			content.add( btn );
			btn.setRect( (i % 2) * (bw + GAP), pos, bw, BTN_H );
			if (i % 2 == 1) {
				pos += BTN_H + GAP;
			}
			i++;
		}
		if (i % 2 == 1) {
			pos += BTN_H + GAP;
		}
		return pos;
	}

	/** Локализованное имя баффа; если строки нет — простое имя класса */
	private static String buffName( Class<? extends Buff> cls ) {
		try {
			Buff b = (Buff) Reflection.newInstance( cls );
			String n = b.name();
			if (n != null && !n.contains( "NO TEXT" )) {
				return n;
			}
		} catch (Exception ignored) {
		}
		return cls.getSimpleName();
	}

	// ==================== Вкладка «Мобы» ====================

	private float buildMobsTab( Component content, float w ) {
		float pos = 0;

		final TrainerSpinner spCount = new TrainerSpinner( "Количество (кап 100)", 1, 100, 1 );
		spCount.setRect( 0, pos, w, 0 );
		content.add( spCount );
		pos = spCount.bottom() + 4;

		final CheckBox allyBox = new CheckBox( "Спавнить союзниками" ) {
			@Override
			protected void onClick() {
				super.onClick();
				Trainer.log( "mob ally mode " + (checked() ? "ON" : "OFF") );
			}
		};
		allyBox.setRect( 0, pos, w, BTN_H );
		content.add( allyBox );
		pos += BTN_H + GAP;

		for (int s = 0; s < MOB_SECTION_NAMES.length; s++) {
			pos = addTabHeader( content, pos, MOB_SECTION_NAMES[s] );
			//последняя секция («Союзники») всегда спавнит как ALLY
			boolean forceAlly = (s == MOB_SECTION_NAMES.length - 1);
			pos = addMobGrid( content, pos, w, MOB_SECTIONS[s], allyBox, spCount, forceAlly );
		}

		pos = cheatRow( content, pos, w, "Убрать всех мобов", Trainer::clearMobs );
		return pos;
	}

	private float addMobGrid( Component content, float pos, float w, Class<?>[] classes,
	                          final CheckBox allyBox, final TrainerSpinner spCount, final boolean forceAlly ) {
		float bw = (w - GAP) / 2f;
		int i = 0;
		for (final Class<?> c : classes) {
			String name = mobName( c );
			Image icon = null;
			try {
				Mob m = (Mob) Reflection.newInstance( c );
				if (m.spriteClass != null) {
					CharSprite sprite = Reflection.newInstance( m.spriteClass );
					sprite.idle();
					icon = sprite;
				}
			} catch (Exception ignored) {
			}

			RedButton btn = new RedButton( name, 7 ) {
				@Override
				protected void onClick() {
					Trainer.spawnMob( c, forceAlly || allyBox.checked(), spCount.value() );
				}
			};
			if (icon != null) {
				btn.icon( icon );
			}
			content.add( btn );
			btn.setRect( (i % 2) * (bw + GAP), pos, bw, BTN_H );
			if (i % 2 == 1) {
				pos += BTN_H + GAP;
			}
			i++;
		}
		if (i % 2 == 1) {
			pos += BTN_H + GAP;
		}
		return pos;
	}

	/** Локализованное имя моба с фолбэком */
	private static String mobName( Class<?> c ) {
		try {
			Mob m = (Mob) Reflection.newInstance( c );
			String n = m.name();
			if (n != null && !n.contains( "NO TEXT" )) {
				return n;
			}
		} catch (Exception ignored) {
		}
		return c.getSimpleName();
	}

	// ==================== Вкладка «Данж» ====================

	private float buildDungeonTab( Component content, final float w ) {
		float pos = 0;

		final TrainerSpinner spDepth = new TrainerSpinner( "Этаж", 1, 25, Dungeon.depth );
		spDepth.setRect( 0, pos, w, 0 );
		content.add( spDepth );
		pos = spDepth.bottom() + 4;

		pos = cheatRow( content, pos, w, "Телепорт на этаж", () -> Trainer.teleportToDepth( spDepth.value() ) );

		//телепорт в точку: окно закрывается, дальше выбирается клетка мышью
		RedButton btnPoint = new RedButton( "Телепорт в точку (выбор клетки)" ) {
			@Override
			protected void onClick() {
				hide();
				Trainer.teleportToPoint();
			}
		};
		content.add( btnPoint );
		btnPoint.setRect( 0, pos, w, BTN_H );
		pos += BTN_H + GAP;

		pos = cheatRow( content, pos, w, "К выходу этажа", Trainer::teleportToExit );
		pos = cheatRow( content, pos, w, "К входу этажа", Trainer::teleportToEntrance );

		RenderedTextBlock seedLabel = PixelScene.renderTextBlock(
				"Seed: " + (Dungeon.seed == 0 ? "случайный" : Dungeon.seed), 7 );
		seedLabel.setPos( 0, pos );
		content.add( seedLabel );
		pos += seedLabel.height() + 2;

		pos = cheatRow( content, pos, w, "Задать seed", () -> GameScene.show( new WndTextInput(
				"Seed", "Влияет на генерацию следующих этажей. 0 — случайный.",
				Dungeon.seed == 0 ? "" : String.valueOf( Dungeon.seed ), 20,
				false, "Set", "Cancel" ) {
			@Override
			public void onSelect( boolean positive, String text ) {
				if (!positive) {
					return;
				}
				try {
					Trainer.setSeed( Long.parseLong( text.trim() ) );
				} catch (NumberFormatException e) {
					Trainer.setSeed( 0 );
				}
				hide();
				GameScene.show( new WndTrainer() );
			}
		} ) );
		pos = cheatRow( content, pos, w, "Случайный seed", () -> Trainer.setSeed( 0 ) );
		return pos;
	}

	// ==================== Вкладка «Инфо» (инспектор) ====================

	private float buildInfoTab( Component content, float w ) {
		float pos = 0;

		//живая панель: сама обновляется ~3 раза в секунду
		InspectorPanel panel = new InspectorPanel();
		panel.setRect( 0, pos, w, 0 );
		content.add( panel );
		pos = panel.bottom() + 6;

		//осмотр клетки: окно закрывается, включается прицел
		RedButton btnInspect = new RedButton( "Осмотреть клетку (выбор)" ) {
			@Override
			protected void onClick() {
				hide();
				Trainer.inspectCell();
			}
		};
		content.add( btnInspect );
		btnInspect.setRect( 0, pos, w, BTN_H );
		pos += BTN_H + GAP;

		pos = cheatRow( content, pos, w, "Сбросить панель (пересоздать)", new Runnable() {
			@Override
			public void run() {
				hide();
				GameScene.show( new WndTrainer() );
			}
		} );
		return pos;
	}

	/** Панель живого состояния: обновляет текст каждые ~20 кадров */
	private static class InspectorPanel extends Component {

		private RenderedTextBlock text;
		private int frames;

		@Override
		protected void createChildren() {
			text = PixelScene.renderTextBlock( Trainer.stateReport(), 7 );
			add( text );
		}

		@Override
		protected void layout() {
			text.maxWidth( (int) width );
			text.setPos( x, y );
			height = text.height();
		}

		@Override
		public synchronized void update() {
			super.update();
			if (++frames % 20 == 0) {
				text.text( Trainer.stateReport() );
				//высота могла измениться (баффы добавились/снялись)
				text.maxWidth( (int) width );
				layout();
			}
		}
	}

	// ==================== Общие помощники ====================

	private float cheatRow( Component content, float pos, float w, String label, final Runnable action ) {
		RedButton btn = new RedButton( label ) {
			@Override
			protected void onClick() {
				action.run();
			}
		};
		btn.setRect( 0, pos, w, BTN_H );
		content.add( btn );
		return pos + BTN_H + GAP;
	}

	private float addTabHeader( Component content, float pos, String title ) {
		RenderedTextBlock head = PixelScene.renderTextBlock( title, 8 );
		head.hardlight( Window.TITLE_COLOR );
		head.setPos( 3, pos + 1 );
		content.add( head );
		return pos + Math.max( 12, head.height() + 3 );
	}

	private static Item newItem( Class<?> cls ) {
		try {
			return (Item) cls.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	// ==================== Прокрутка ====================

	/** ScrollPane с самолечением: каждый кадр принудительно восстанавливает
	 *  позицию панели и якорь камеры контента (левый-верхний угол).
	 *  Без этого какой-то механизм игры смещает камеру контента
	 *  ровно на пол-окна, и содержимое «уезжает» из рамки. */
	public static class RefreshableScrollPane extends ScrollPane {

		private int frames;
		private float homeX = Float.NaN, homeY;

		public RefreshableScrollPane( Component content ) {
			super( content );
		}

		public void refresh() {
			layout();
		}

		@Override
		public synchronized void update() {
			if (parent != null) {
				if (Float.isNaN( homeX )) {
					homeX = x;
					homeY = y;
				}
				x = homeX;
				y = homeY;
				Camera wc = camera();
				if (wc != null) {
					Point p = wc.cameraToScreen( x, y );
					content.camera.x = p.x;
					content.camera.y = p.y;
					content.camera.resize( (int) width, (int) height );
					content.camera.scroll.set(
							GameMath.gate( 0f, content.camera.scroll.x,
									Math.max( 0f, content.width() - width ) ),
							GameMath.gate( 0f, content.camera.scroll.y,
									Math.max( 0f, content.height() - height ) ) );
				}
			}
			super.update();
		}
	}

	/** Иконка предмета-кнопки для сетки */
	private static class ItemIconButton extends Button {

		private final Item item;
		private final ItemSprite sprite;

		ItemIconButton( Item item ) {
			super();
			this.item = item;
			sprite = new ItemSprite( item );
			add( sprite );
		}

		@Override
		protected void layout() {
			super.layout();
			sprite.x = x + (width - sprite.width()) / 2f;
			sprite.y = y + (height - sprite.height()) / 2f;
			PixelScene.align( sprite );
		}

		@Override
		protected void onClick() {
			Trainer.log( "item clicked: " + item.name() );
			GameScene.show( new WndTrainerItem( item ) );
		}
	}
}
