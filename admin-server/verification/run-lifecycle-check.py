from pathlib import Path
import zipfile,subprocess,os
out=Path(__file__).resolve().parent
if not os.environ.get('MYSQL_PWD') or not os.environ.get('JAVA_HOME'): raise SystemExit('Set JAVA_HOME and MYSQL_PWD; run only against the local development database.')
libs=out/'.lifecycle-libs';libs.mkdir(exist_ok=True)
with zipfile.ZipFile(out.parent/'ruoyi-admin/target/ruoyi-admin.jar') as z:
 for name in z.namelist():
  if name.startswith('BOOT-INF/lib/') and name.endswith('.jar'):(libs/Path(name).name).write_bytes(z.read(name))
cp=str(libs/'*')+os.pathsep+str(out)+os.pathsep+str(out.parent/'ruoyi-admin/target/classes')
subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/javac.exe'),'-encoding','UTF-8','-sourcepath',str(out/'empty-source'),'-cp',cp,str(out/'LifecycleCheck.java')],check=True)
subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/java.exe'),'-cp',cp,'LifecycleCheck'],check=True)
