using System.Windows;
using System.Windows.Automation;
using System.Windows.Controls;

namespace GameBox.Windows;

public sealed class LaunchMetadataDialog : Window
{
    private readonly TextBox _titleBox;
    private readonly TextBox _platformBox;
    private readonly TextBox _argumentsBox;

    public string GameTitle => _titleBox.Text;
    public string Platform => _platformBox.Text;
    public string Arguments => _argumentsBox.Text;

    public LaunchMetadataDialog(string title, string platform, string arguments)
    {
        Title = "Edit game details";
        Width = 520;
        SizeToContent = SizeToContent.Height;
        ResizeMode = ResizeMode.NoResize;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;

        var panel = new StackPanel { Margin = new Thickness(24) };
        panel.Children.Add(new TextBlock { Text = "Title" });
        _titleBox = new TextBox { Text = title, Margin = new Thickness(0, 6, 0, 14) };
        AutomationProperties.SetName(_titleBox, "Game title");
        panel.Children.Add(_titleBox);

        panel.Children.Add(new TextBlock { Text = "Platform" });
        _platformBox = new TextBox { Text = platform, Margin = new Thickness(0, 6, 0, 14) };
        AutomationProperties.SetName(_platformBox, "Game platform");
        panel.Children.Add(_platformBox);

        panel.Children.Add(new TextBlock { Text = "Launch arguments" });
        _argumentsBox = new TextBox { Text = arguments, Margin = new Thickness(0, 6, 0, 18) };
        AutomationProperties.SetName(_argumentsBox, "Launch arguments");
        panel.Children.Add(_argumentsBox);

        var actions = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
        var cancel = new Button { Content = "Cancel", IsCancel = true, MinWidth = 90 };
        var save = new Button { Content = "Save", IsDefault = true, MinWidth = 90, Margin = new Thickness(10, 0, 0, 0) };
        save.Click += (_, _) => { DialogResult = true; Close(); };
        actions.Children.Add(cancel);
        actions.Children.Add(save);
        panel.Children.Add(actions);

        Content = panel;
        Loaded += (_, _) => { _titleBox.Focus(); _titleBox.SelectAll(); };
    }
}
