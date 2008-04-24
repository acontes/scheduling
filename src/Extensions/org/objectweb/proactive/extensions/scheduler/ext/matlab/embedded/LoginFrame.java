package org.objectweb.proactive.extensions.scheduler.ext.matlab.embedded;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import javax.security.auth.login.LoginException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.objectweb.proactive.extensions.scheduler.common.exception.SchedulerException;


/**
 * Class used to display a login window under Matlab for prompting login and passwords
 * @author The ProActive Team
 *
 */
public class LoginFrame extends JDialog {

    private JTextField username;
    private JPasswordField password;
    private AOMatlabEnvironment aome;
    private JButton login;
    private boolean loginSuccessful = false;

    /**
     * Creates a new LoginFrame
     */
    public LoginFrame(AOMatlabEnvironment aome) {
        this.aome = aome;

        initComponents();

        pack();

        // Center the component
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension size = tk.getScreenSize();

        int x = (size.width / 2) - (getWidth() / 2);
        int y = (size.height / 2) - (getHeight() / 2);

        setLocation(x, y);
    }

    public void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel cp = new JPanel(new GridBagLayout());
        cp.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel usernameLabel = new JLabel("Username:");
        cp.add(usernameLabel, getConstraints(0, 0, 1, 1));

        username = new JTextField(15);
        cp.add(username, getConstraints(1, 0, 1, 1));

        JLabel passwordLabel = new JLabel("Password:");
        cp.add(passwordLabel, getConstraints(0, 1, 1, 1));

        password = new JPasswordField(15);
        cp.add(password, getConstraints(1, 1, 1, 1));

        login = new JButton("Login");

        cp.add(login, getConstraints(1, 2, 1, 1));

        setContentPane(cp);
    }

    public void start() {
        this.setVisible(true);
    }

    public JButton getLoginButton() {
        return login;
    }

    public boolean checkLogin() {
        String name = username.getText();
        String pwd = new String(password.getPassword());

        try {
            aome.login(name, pwd);
            dispose();
            return true;
        } catch (LoginException ex) {
            JOptionPane.showMessageDialog(LoginFrame.this, "Incorrect username/password combination.",
                    "Login Error", JOptionPane.ERROR_MESSAGE);
        } catch (SchedulerException ex2) {
            JOptionPane.showMessageDialog(LoginFrame.this, ex2.getMessage(), "Login Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        return false;
    }

    private GridBagConstraints getConstraints(int x, int y, int width, int height) {
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width;
        constraints.gridheight = height;
        constraints.anchor = constraints.CENTER;
        constraints.fill = constraints.BOTH;

        return constraints;
    }

}
