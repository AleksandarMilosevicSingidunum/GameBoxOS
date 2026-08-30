using System.Windows;
using System.Windows.Automation;
using System.Windows.Controls;

namespace GameBox.Windows;

public sealed class MoonlightSessionDialog : Window
{
    private readonly TextBox _hostBox;
    private readonly TextBox _applicationBox;

    public string Host => _hostBox.Text;
    public string ApplicationName => _applicationBox.Text;

    public MoonlightSessionDialog()
    {
        Title = "Add Moonlight session";
        Width = 520;
        SizeToContent = SizeToContent.Height;
        ResizeMode = ResizeMode.NoResize;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;

        var panel = new StackPanel { Margin = new Thickness(24) };
        panel.Children.Add(new TextBlock { Text = "PC host or IP address" });
        _hostBox = new TextBox { Margin = new Thickness(0, 6, 0, 14) };
        AutomationProperties.SetName(_hostBox, "Moonlight PC host");
        panel.Children.Add(_hostBox);

        panel.Children.Add(new TextBlock { Text = "Moonlight application name" });
        _applicationBox = new TextBox { Margin = new Thickness(0, 6, 0, 18) };
        AutomationProperties.SetName(_applicationBox, "Moonlight application name");
        panel.Children.Add(_applicationBox);

        panel.Children.Add(new TextBlock {
            Text = "GameBox stores only the host and application as launch arguments. Pairing and credentials remain managed by Moonlight.",
            TextWrapping = TextWrapping.Wrap,
            Margin = new Thickness(0, 0, 0, 18)
        });

        var actions = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
        var cancel = new Button { Content = "Cancel", IsCancel = true, MinWidth = 90 };
        var add = new Button { Content = "Add", IsDefault = true, MinWidth = 90, Margin = new Thickness(10, 0, 0, 0) };
        add.Click += (_, _) => { DialogResult = true; Close(); };
        actions.Children.Add(cancel);
        actions.Children.Add(add);
        panel.Children.Add(actions);
        Content = panel;
        Loaded += (_, _) => _hostBox.Focus();
    }
}
