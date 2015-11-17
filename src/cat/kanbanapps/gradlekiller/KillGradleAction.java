package cat.kanbanapps.gradlekiller;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class KillGradleAction extends AnAction {
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        String[] pids;
        try {
            pids = getPids();
        } catch (UnsupportedOperationException e) {
            Messages.showMessageDialog(project, "Could not get process data!", "Gradle Killer", Messages.getErrorIcon());
            return;
        }

        if (pids.length == 0) {
            Messages.showMessageDialog(project, "No Gradle process is running!", "Gradle Killer", Messages.getErrorIcon());
        } else {
            boolean result = true;
            for (String pid : pids) {
                result = result & killProcess(pid);
            }
            if (result) {
                Messages.showMessageDialog(project, "Gradle was killed! Your IDE may show you some other dialogs, it's safe to ignore them.", "Gradle Killer", Messages.getInformationIcon());
            } else {
                Messages.showMessageDialog(project, "Could not kill gradle! Check that your system supports killing processes!", "Gradle Killer", Messages.getErrorIcon());
            }
        }
    }

    private String[] getPids() throws UnsupportedOperationException {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return getPidsOnWindows();
        } else {
            return getPidsOnUnix();
        }
    }

    /**
     * @param pid The PID of the process to kill
     * @return true if killed, false if not
     */
    private boolean killProcess(String pid) {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return killProcessOnWindows(pid);
        } else {
            return killProcessOnUnix(pid);
        }
    }

    private String[] getPidsOnUnix() throws UnsupportedOperationException {
        ArrayList<String> pids = new ArrayList<String>();
        Runtime r = Runtime.getRuntime();
        Process p;
        try {
            p = r.exec("pgrep -f gradle-launcher");
            p.waitFor();

            if (p.exitValue() != 0 && p.exitValue() != 1) { //OK found, OK not found
                throw new UnsupportedOperationException("pgrep returned error value!");
            } else {
                BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;

                while ((line = b.readLine()) != null) {
                    pids.add(line);
                }

                b.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("pgrep parsing failed!");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("pgrep parsing failed!");
        }
        return pids.toArray(new String[pids.size()]);
    }

    private String[] getPidsOnWindows() throws UnsupportedOperationException {
        ArrayList<String> pids = new ArrayList<String>();
        Runtime r = Runtime.getRuntime();
        Process p;
        try {
            p = r.exec("wmic process where \"commandline like '%gradle-launcher%' and name like '%java%'\" get processid");
            p.waitFor();
            BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            while ((line = b.readLine()) != null) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Integer.parseInt(line.trim());
                    pids.add(line.trim());
                } catch (NumberFormatException e) {
                    //Don't add it, it's a string!
                }
            }

            b.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("wmic parsing failed!");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("wmic parsing failed!");
        }
        return pids.toArray(new String[pids.size()]);
    }

    private boolean killProcessOnWindows(String pid) {
        Runtime r = Runtime.getRuntime();
        Process p;
        try {
            p = r.exec("taskkill /F /PID " + pid);
            p.waitFor();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean killProcessOnUnix(String pid) {
        Runtime r = Runtime.getRuntime();
        Process p;
        try {
            p = r.exec("kill -9 " + pid);
            p.waitFor();

            return p.exitValue() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
