/*
 * Тренер для Shattered Pixel Dungeon v4.0.2 (SPD Trainer project).
 * Логика читов: состояние и действия над героем.
 *
 * Все методы вызываются из обработчиков кнопок окон трейнера,
 * то есть в главном потоке игры — прямые обращения к состоянию
 * подземелья здесь безопасны.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.shatteredpixel.shatteredpixeldungeon.trainer;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.watabou.noosa.Game;
import com.watabou.utils.Reflection;
import com.watabou.utils.SparseArray;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class Trainer {

	/** Включён ли режим бога (перехват урона в Char.damage) */
	public static boolean godMode = false;

	/** Вывод в консоль для диагностики трейнера */
	static void log( String msg ) {
		System.out.println( "[SPD-TRAINER] " + msg );
	}

	public static boolean inGame() {
		return Dungeon.hero != null && Dungeon.hero.isAlive();
	}

	/** Герой для вызовов applyBuff(target, ...) из UI */
	public static Hero hero() {
		return Dungeon.hero;
	}

	/** Полное здоровье */
	public static void fullHealth() {
		Dungeon.hero.HP = Dungeon.hero.HT;
		log( "full health: HP=" + Dungeon.hero.HP + "/" + Dungeon.hero.HT );
	}

	/** Добавить золото */
	public static void addGold( int amount ) {
		Dungeon.gold += amount;
		log( "gold +" + amount + " -> " + Dungeon.gold );
	}

	/** Мгновенно закрыть текущий уровень опыта */
	public static void levelUp() {
		Hero hero = Dungeon.hero;
		hero.earnExp( hero.maxExp() - hero.exp, Trainer.class );
		log( "level up: lvl=" + hero.lvl + " exp=" + hero.exp + "/" + hero.maxExp() );
	}

	/** +1 к базовой силе */
	public static void addStrength() {
		Dungeon.hero.STR++;
		log( "strength +1 -> " + Dungeon.hero.STR );
	}

	/** Убрать голод до следующего сытного действия */
	public static void removeHunger() {
		Buff.detach( Dungeon.hero, Hunger.class );
		log( "hunger removed" );
	}

	/** Открыть всю карту текущего этажа */
	public static void revealMap() {
		Level level = Dungeon.level;
		for (int i = 0; i < level.length(); i++) {
			level.visited[i] = true;
			level.mapped[i] = true;
		}
		GameScene.updateFog();
		log( "map revealed: " + level.length() + " cells" );
	}

	/** Перезарядить все жезлы в инвентаре */
	public static void chargeWands() {
		int charged = 0;
		for (Item item : Dungeon.hero.belongings) {
			if (item instanceof Wand) {
				Wand wand = (Wand) item;
				wand.curCharges = wand.maxCharges;
				wand.updateQuickslot();
				charged++;
			}
		}
		log( "recharged " + charged + " wands" );
	}

	/** Выдать предмет герою; при переполнении инвентаря — бросить под ноги.
	 *  Метательные «сеты» с количеством выше defaultQuantity() игра при
	 *  подборе сливает и обрезает до 3 — поэтому заранее режем на наборы. */
	public static void giveItem( Item item ) {
		if (item instanceof MissileWeapon) {
			int def = ((MissileWeapon) item).defaultQuantity();
			while (item.quantity() > def) {
				giveOne( item.split( def ) );
			}
		}
		giveOne( item );
	}

	private static void giveOne( Item item ) {
		if (!item.collect( Dungeon.hero.belongings.backpack )) {
			Dungeon.level.drop( item, Dungeon.hero.pos ).sprite.drop();
			log( "gave (dropped on floor): " + item.name() + " x" + item.quantity() );
		} else {
			log( "gave: " + item.name() + " x" + item.quantity() );
		}
		Item.updateQuickslot();
	}

	// ==================== Герой ====================

	/** Задать текущее здоровье героя */
	public static void setHP( int hp ) {
		Dungeon.hero.HP = Math.max( 1, Math.min( hp, Dungeon.hero.HT ) );
		log( "hero HP = " + Dungeon.hero.HP + "/" + Dungeon.hero.HT );
	}

	/** Задать максимум здоровья */
	public static void setHT( int ht ) {
		Dungeon.hero.HT = Math.max( 1, ht );
		Dungeon.hero.HP = Math.min( Dungeon.hero.HP, Dungeon.hero.HT );
		log( "hero HT = " + Dungeon.hero.HT );
	}

	/** Задать уровень героя (с пересчётом HT как при левел-апе) */
	public static void setLevel( int lvl ) {
		Dungeon.hero.lvl = Math.max( 1, Math.min( 30, lvl ) );
		Dungeon.hero.updateHT( false );
		Dungeon.hero.HP = Math.min( Dungeon.hero.HP, Dungeon.hero.HT );
		log( "hero lvl = " + Dungeon.hero.lvl );
	}

	/** Задать силу героя */
	public static void setSTR( int str ) {
		Dungeon.hero.STR = Math.max( 1, Math.min( 30, str ) );
		log( "hero STR = " + Dungeon.hero.STR );
	}

	/** Задать точную стадию голода: 0 сыт, 300 голоден, 450 голодает */
	public static void setHungerStage( int stage ) {
		Hunger hunger = Dungeon.hero.buff( Hunger.class );
		if (hunger == null) {
			hunger = Buff.affect( Dungeon.hero, Hunger.class );
		}
		try {
			Field level = Hunger.class.getDeclaredField( "level" );
			level.setAccessible( true );
			level.setFloat( hunger, stage );
			log( "hunger level = " + stage );
		} catch (Exception e) {
			log( "hunger stage FAILED: " + e );
		}
	}

	/** Сбросить все потраченные таланты */
	public static void resetTalents() {
		for (LinkedHashMap<Talent, Integer> tier : Dungeon.hero.talents) {
			tier.clear();
		}
		log( "talents reset" );
	}

	// ==================== Баффы ====================

	/** Наложить бафф на цель. Спец-случаи: Barrier (щит) и Healing (пул лечения)
	 *  принимают «значение» вместо длительности; FlavourBuff — длительность. */
	public static void applyBuff( Char target, Class<? extends Buff> cls, float value ) {
		try {
			if (cls == Barrier.class) {
				Buff.affect( target, Barrier.class ).setShield( (int) value );
				log( "buff " + cls.getSimpleName() + " on " + target.getClass().getSimpleName()
						+ ": shield=" + (int) value );
			} else if (cls == Healing.class) {
				Buff.affect( target, Healing.class ).setHeal( (int) value, 0f, 1 );
				log( "buff Healing on " + target.getClass().getSimpleName()
						+ ": heal pool=" + (int) value );
			} else if (FlavourBuff.class.isAssignableFrom( cls )) {
				Buff.affect( target, cls.asSubclass( FlavourBuff.class ), value );
				log( "buff " + cls.getSimpleName() + " on "
						+ target.getClass().getSimpleName() + " (" + (long) value + " turns)" );
			} else {
				Buff.affect( target, cls );
				log( "buff " + cls.getSimpleName() + " on "
						+ target.getClass().getSimpleName() );
			}
		} catch (Exception e) {
			log( "buff FAILED: " + cls.getSimpleName() + " — " + e );
		}
	}

	/** Наложить бафф/дебафф на ближайшего видимого врага */
	public static void applyBuffToNearestMob( Class<? extends Buff> cls, float value ) {
		Mob best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs) {
			if (m.isAlive() && m.alignment == Char.Alignment.ENEMY) {
				int d = Dungeon.level.distance( m.pos, Dungeon.hero.pos );
				if (d < bestDist) {
					bestDist = d;
					best = m;
				}
			}
		}
		if (best == null) {
			log( "no enemy nearby for " + cls.getSimpleName() );
			return;
		}
		log( "targeting nearest enemy: " + best.name() + " (dist " + bestDist + ")" );
		applyBuff( best, cls, value );
	}

	/** Снять все баффы с героя (чистая платформа для теста) */
	public static void removeAllBuffs() {
		int removed = 0;
		for (Buff b : Dungeon.hero.buffs().toArray( new Buff[0] )) {
			b.detach();
			removed++;
		}
		log( "removed " + removed + " buffs" );
	}

	// ==================== Мобы ====================

	/** Заспавнить count мобов рядом с героем.
	 *  ally=true делает моба союзником; иначе сохраняется его собственный
	 *  alignment (враги — врагами, NPC — нейтральными). */
	public static void spawnMob( Class<?> cls, boolean ally, int count ) {
		int spawned = 0;
		Mob first = null;
		for (int i = 0; i < count; i++) {
			Mob mob;
			try {
				mob = (Mob) Reflection.newInstance( cls );
			} catch (Exception e) {
				log( "spawn FAILED: " + cls.getSimpleName() + " — " + e );
				return;
			}
			if (mob == null) {
				return;
			}
			if (ally) {
				mob.alignment = Char.Alignment.ALLY;
			}
			int cell = Dungeon.level.randomRespawnCell( mob );
			if (cell == -1) {
				cell = Dungeon.level.randomDestination( mob );
			}
			if (cell == -1) {
				continue;
			}
			mob.pos = cell;
			GameScene.add( mob );
			if (first == null) {
				first = mob;
			}
			spawned++;
		}
		log( "spawned " + spawned + "x "
				+ (first != null ? first.name() : cls.getSimpleName())
				+ (ally ? " [ally]" : "") );
	}

	/** Убрать всех мобов с текущего этажа (без опыта и лута) */
	public static void clearMobs() {
		int removed = 0;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )) {
			m.destroy();
			Dungeon.level.mobs.remove( m );
			if (m.sprite != null) {
				m.sprite.killAndErase();
			}
			removed++;
		}
		log( "cleared " + removed + " mobs" );
	}

	// ==================== Данж ====================

	/** Телепорт на указанный этаж (герой появляется у входа).
	 *  Идёт через Mode.RETURN: он не требует LevelTransition —
	 *  descend() разыменовал бы наш null и падал с NPE. */
	public static void teleportToDepth( int depth ) {
		if (!inGame() || depth == Dungeon.depth) {
			log( "teleport skipped (same depth or not in game)" );
			return;
		}
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = Math.max( 1, Math.min( 25, depth ) );
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = -1; //по -1 switchLevel ставит героя у входа
		Game.switchScene( InterlevelScene.class );
		log( "teleporting to depth " + depth );
	}

	/** Задать seed генерации (влияет на следующие этажи) */
	public static void setSeed( long seed ) {
		Dungeon.seed = seed;
		log( "seed = " + seed );
	}

	/** Телепорт героя к выходу текущего этажа */
	public static void teleportToExit() {
		ScrollOfTeleportation.appear( Dungeon.hero, Dungeon.level.exit );
		log( "teleported to exit" );
	}

	/** Задать золото */
	public static void setGold( int gold ) {
		Dungeon.gold = Math.max( 0, gold );
		log( "gold = " + Dungeon.gold );
	}

	/** Телепорт героя к входу текущего этажа */
	public static void teleportToEntrance() {
		ScrollOfTeleportation.appear( Dungeon.hero, Dungeon.level.entrance );
		log( "teleported to entrance" );
	}

	/** Телепорт в произвольную точку: включает выбор клетки мышью */
	public static void teleportToPoint() {
		GameScene.selectCell( new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null) {
					return;
				}
				if (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell]) {
					ScrollOfTeleportation.appear( Dungeon.hero, cell );
					log( "teleported to cell " + cell );
				} else {
					log( "teleport to cell " + cell + " rejected (not passable)" );
				}
			}

			@Override
			public String prompt() {
				return "Выберите клетку для телепорта";
			}
		} );
	}

	// ==================== Инспектор ====================

	/** Живой отчёт о состоянии забега (для панели инспектора) */
	public static String stateReport() {
		if (Dungeon.hero == null || Dungeon.level == null) {
			return "Нет активного забега";
		}
		Hero hero = Dungeon.hero;
		Level level = Dungeon.level;
		int w = level.width();

		StringBuilder sb = new StringBuilder();
		sb.append( "Этаж: " ).append( Dungeon.depth )
				.append( " (ветка " ).append( Dungeon.branch ).append( "), " )
				.append( level.getClass().getSimpleName() ).append( '\n' );
		sb.append( "Герой: клетка " ).append( hero.pos )
				.append( " (" ).append( hero.pos % w ).append( "," ).append( hero.pos / w ).append( ")\n" );
		sb.append( "HP: " ).append( hero.HP ).append( "/" ).append( hero.HT )
				.append( ", уровень " ).append( hero.lvl )
				.append( ", сила " ).append( hero.STR() )
				.append( ", опыт " ).append( hero.exp ).append( "/" ).append( hero.maxExp() ).append( '\n' );
		sb.append( "Золото: " ).append( Dungeon.gold )
				.append( " (всего собрано " ).append( Statistics.goldCollected ).append( ")\n" );
		sb.append( "Seed: " ).append( Dungeon.seed == 0 ? "случайный" : Dungeon.seed ).append( '\n' );
		sb.append( "Ходов: " ).append( (long) Actor.now() ).append( '\n' );
		sb.append( "Убито врагов: " ).append( Statistics.enemiesSlain )
				.append( ", глубочайший этаж " ).append( Statistics.deepestFloor ).append( '\n' );

		int enemies = 0, allies = 0, others = 0;
		for (Mob m : level.mobs) {
			if (!m.isAlive()) {
				continue;
			}
			if (m.alignment == Char.Alignment.ENEMY) {
				enemies++;
			} else if (m.alignment == Char.Alignment.ALLY) {
				allies++;
			} else {
				others++;
			}
		}
		sb.append( "Мобы: врагов " ).append( enemies )
				.append( ", союзников " ).append( allies )
				.append( ", прочих " ).append( others ).append( '\n' );
		SparseArray<Heap> heaps = levelHeaps();
		SparseArray<Trap> traps = levelTraps();
		//у gdx IntMap size — поле, не метод
		sb.append( "На полу: " ).append( heaps == null ? "?" : heaps.size ).append( " кучек, " )
				.append( traps == null ? "?" : traps.size ).append( " ловушек\n" );

		sb.append( "Баффы героя:" );
		if (hero.buffs().isEmpty()) {
			sb.append( " нет" );
		} else {
			for (Buff b : hero.buffs()) {
				sb.append( "\n  " ).append( buffDisplayName( b ) );
				float left = b.cooldown();
				if (left > 0) {
					sb.append( " (" ).append( Math.round( left ) ).append( ")" );
				}
			}
		}
		return sb.toString();
	}

	/** Имя баффа с фолбэком на имя класса */
	static String buffDisplayName( Buff b ) {
		try {
			String n = b.name();
			if (n != null && !n.contains( "NO TEXT" )) {
				return n;
			}
		} catch (Exception ignored) {
		}
		return b.getClass().getSimpleName();
	}

	// Level.heaps и Level.traps объявлены без модификатора (package-private):
	// читаем их рефлексией и приводим к публичному SparseArray
	@SuppressWarnings("unchecked")
	private static <T> SparseArray<T> levelField( String name, Class<T> cls ) {
		try {
			Field f = Level.class.getDeclaredField( name );
			f.setAccessible( true );
			Object v = f.get( Dungeon.level );
			if (cls.isInstance( v ) || v instanceof SparseArray) {
				return (SparseArray<T>) v;
			}
		} catch (Exception e) {
			log( "level field '" + name + "' unavailable: " + e );
		}
		return null;
	}

	public static SparseArray<Heap> levelHeaps() {
		return levelField( "heaps", Heap.class );
	}

	public static SparseArray<Trap> levelTraps() {
		return levelField( "traps", Trap.class );
	}

	/** Осмотр клетки мышью: показывает отладочное окно с данными клетки */
	public static void inspectCell() {
		GameScene.selectCell( new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null) {
					return;
				}
				log( "inspect cell " + cell );
				GameScene.show( new WndInspectCell( cellReport( cell ) ) );
			}

			@Override
			public String prompt() {
				return "Выберите клетку для осмотра";
			}
		} );
	}

	/** Отладочная сводка по клетке: тайл, ловушка, предметы, моб */
	public static String cellReport( int cell ) {
		Level level = Dungeon.level;
		int w = level.width();

		StringBuilder sb = new StringBuilder();
		sb.append( "Клетка " ).append( cell )
				.append( " (" ).append( cell % w ).append( "," ).append( cell / w ).append( ")\n" );

		sb.append( "Тайл #" ).append( level.map[cell] );
		if (level.passable[cell]) {
			sb.append( ", проходимый" );
		}
		if (level.visited[cell]) {
			sb.append( ", разведан" );
		} else {
			sb.append( ", не разведан" );
		}
		sb.append( level.heroFOV[cell] ? ", в поле зрения" : ", вне поля зрения" ).append( '\n' );

		Trap trap = null;
		SparseArray<Trap> traps = levelTraps();
		if (traps != null) {
			trap = traps.get( cell );
		}
		if (trap != null) {
			sb.append( "Ловушка: " ).append( trap.name() )
					.append( trap.visible ? "" : " [скрыта]" )
					.append( trap.active ? "" : " [неактивна]" ).append( '\n' );
		}

		Heap heap = null;
		SparseArray<Heap> heaps = levelHeaps();
		if (heaps != null) {
			heap = heaps.get( cell );
		}
		if (heap != null) {
			sb.append( "Предметы:" );
			for (Item it : heap.items) {
				sb.append( "\n  " ).append( it.name() ).append( " x" ).append( it.quantity() );
			}
			sb.append( '\n' );
		}

		Mob mob = null;
		for (Mob m : level.mobs) {
			if (m.pos == cell && m.isAlive()) {
				mob = m;
				break;
			}
		}
		if (mob != null) {
			sb.append( "Моб: " ).append( mob.name() )
					.append( " " ).append( mob.HP ).append( "/" ).append( mob.HT )
					.append( " (" ).append( mob.alignment ).append( ")\n" );
			if (!mob.buffs().isEmpty()) {
				sb.append( "  баффы:" );
				for (Buff b : mob.buffs()) {
					sb.append( " " ).append( buffDisplayName( b ) );
				}
				sb.append( '\n' );
			}
		}
		return sb.toString();
	}
}
