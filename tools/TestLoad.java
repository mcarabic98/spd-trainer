import java.lang.reflect.Method;

public class TestLoad {
	public static void main(String[] args) throws Exception {
		String[] names = {
			"com.shatteredpixel.shatteredpixeldungeon.actors.Char",
			"com.shatteredpixel.shatteredpixeldungeon.windows.WndGame",
			"com.shatteredpixel.shatteredpixeldungeon.trainer.Trainer",
			"com.shatteredpixel.shatteredpixeldungeon.trainer.TrainerSpinner",
			"com.shatteredpixel.shatteredpixeldungeon.trainer.WndTrainer",
			"com.shatteredpixel.shatteredpixeldungeon.trainer.WndTrainerItem",
			"com.shatteredpixel.shatteredpixeldungeon.trainer.WndInspectCell"
		};
		for (String n : names) {
			Class<?> c = Class.forName(n, false, TestLoad.class.getClassLoader());
			System.out.println("OK  " + c.getName()
				+ "  <- " + c.getProtectionDomain().getCodeSource().getLocation());
		}
		// god mode доступен и читается из пропатченного Char
		Class<?> t = Class.forName("com.shatteredpixel.shatteredpixeldungeon.trainer.Trainer", true, TestLoad.class.getClassLoader());
		Object m = t.getField("godMode").get(null);
		System.out.println("Trainer.godMode = " + m);
		System.out.println("ALL CLASSES LINKED");
	}
}
