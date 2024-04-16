import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import matplotlib.ticker as ticker


class DataPlotter:
    def __init__(self, dfs):
        self.dfs = dfs
        self.colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728']

    def plot_cpu_usage(self):
        self._plot_metric('cpu_usage', 'CPU usage (%)')

    # total_power_values = []
    # for df in self.dfs:
    #     total_power_summed = df['power_total'].sum() / 1000000000  # convert to TWh
    #     total_power_values.append(total_power_summed)
    #
    # models = ['sqrt', 'linear', 'square', 'cubic']  # Example model names
    #
    # plt.figure(figsize=(10, 8))  # Adjusted for similarity to the example
    # colors = np.random.rand(len(models), 3)  # Generate random colors
    # bars = plt.barh(models, total_power_values, color=colors, edgecolor='black')
    #
    # # Adding the text labels next to the bars
    # for bar in bars:
    #     width = bar.get_width()
    #     plt.text(width, bar.get_y() + bar.get_height() / 2, f'{width:.2f} TWh', va='center')
    #
    # plt.xlabel('Total Energy Consumed [TWh]')  # Adjusted unit to TWh
    # plt.ylabel('Method')
    # plt.title('Total Power for each model')
    # plt.show()
    def plot_power_total_bars(self):
        total_power_values = [df['power_total'].sum() / 1e9 for df in self.dfs]  # convert to TWh

        models = ['sqrt', 'linear', 'square', 'cubic']
        plt.figure(figsize=(12, 10))

        # Create the horizontal bar chart
        bars = plt.barh(models, total_power_values, color=self.colors, edgecolor='black')

        # Adding the text labels next to the bars
        for bar in bars:
            width = bar.get_width()
            plt.text(width, bar.get_y() + bar.get_height() / 2, f'{width:.2f} TWh', va='center')

        plt.xlabel('Total Energy Consumed [TWh]', fontsize=16)
        plt.ylabel('Model', fontsize=16)
        plt.title('Total Power for each model', fontsize=16)
        plt.show()

    def plot_power_total(self):
        self._plot_metric_individually('power_total', 'Total Power (W)')
        self._plot_multi_metric('power_total', 'Total Power (W)')
        self._plot_metric('power_total', 'Total Power (W)')

    def plot_cpu_limit(self):
        self._plot_metric('cpu_limit', 'CPU Limit (%)')

    def plot_cpu_demand(self):
        self._plot_metric('cpu_demand', 'CPU Demand (%)')

    def plot_cpu_utilization(self):
        metric_name = 'cpu_utilization'
        y_label = 'CPU Utilization (%)'
        metric_values = []
        for df in self.dfs:  # iterate over the list of DataFrames
            if metric_name in df.columns:
                metric_values.append(np.multiply(df[metric_name], 100))
            else:
                print(f"DataFrame does not have '{metric_name}' column")
                metric_values.append(pd.Series([None]))  # Handle missing data

        self._plot_the_plot(metric_values, y_label)

    def plot_cpu_time_active(self):
        self._plot_metric('cpu_time_active', 'CPU Time Active (s)')

    def plot_cpu_time_idle(self):
        self._plot_metric('cpu_time_idle', 'CPU Time Idle (s)')

    def plot_cpu_time_steal(self):
        self._plot_metric('cpu_time_steal', 'CPU Time Steal (s)')

    def plot_cpu_time_lost(self):
        self._plot_metric('cpu_time_lost', 'CPU Time Lost (s)')

    def plot_servers_active(self):
        self._plot_metric('servers_active', 'Servers Active')

    def plot_attempts_success(self):
        self._plot_metric('attempts_success', 'Attempts Success')

    def plot_mem_capacity(self):
        self._plot_metric('mem_capacity', 'Memory Capacity')

    def plot_cpu_count(self):
        self._plot_metric('cpu_count', 'CPU Count')

    def plot_cpu_limit(self):
        self._plot_metric('cpu_limit', 'CPU Limit (%)')

    def plot_guests_running(self):
        self._plot_metric('guests_running', 'Guests Running')

    def plot_guests_terminated(self):
        self._plot_metric('guests_terminated', 'Guests Terminated')

    def plot_guests_error(self):
        self._plot_metric('guests_error', 'Guests Error')

    def plot_guests_invalid(self):
        self._plot_metric('guests_invalid', 'Guests Invalid')

    def plot_attempts_failure(self):
        self._plot_metric('attempts_failure', 'Attempts Failure')

    def plot_attempts_error(self):
        self._plot_metric('attempts_error', 'Attempts Error')

    def plot_hosts_up(self):
        self._plot_metric('hosts_up', 'Hosts Up')

    def plot_hosts_down(self):
        self._plot_metric('hosts_down', 'Hosts Down')

    def plot_servers_pending(self):
        self._plot_metric('servers_pending', 'Servers Pending')

    def _plot_metric(self, metric_name, y_label):
        metric_values = []
        for df in self.dfs:  # iterate over the list of DataFrames
            if metric_name in df.columns:
                metric_values.append(df[metric_name])
            else:
                print(f"DataFrame does not have '{metric_name}' column")
                metric_values.append(pd.Series([None]))  # Handle missing data

        self._plot_the_plot(metric_values, y_label)
        # plt.figure(figsize=(10, 10))
        # for i, metric_value in enumerate(metric_values):
        #     plt.plot(metric_value, label=f'Model {i+1}')  # Add label for each line for the legend
        # plt.xlabel('Time (s)', fontsize=16)  # Increase font size
        # plt.ylabel(y_label, fontsize=16)  # Increase font size
        # plt.title(f'{y_label} for each model', fontsize=16)  # Increase font size
        # plt.legend()  # Add legend to the plot
        # plt.show()

    def _plot_multi_metric(self, metric_name, y_label):
        model_names = ['SQRT', 'LINEAR', 'SQUARE', 'CUBIC']
        colors = ['#0E2A7A', '#F5D34B', '#BD2D2F', '#0E2A7A']
        markers = ['^', 's', 'o', 'x']
        linestyles = ['-', '--', ':']  # Different line styles for each model
        plt.figure(figsize=(20, 10))
        for i, df in enumerate(self.dfs):
            if i == 3:
                break
            plt.plot(np.divide(df[metric_name][:40], 1000), label=f'MODEL {model_names[i]}', color=colors[i],
                     marker=markers[i], markersize=15, markevery=5, linewidth=3, linestyle=linestyles[i])

        plt.xlabel('Time (s)', fontsize=32)
        plt.ylabel("Energy (kWh)", fontsize=32)
        plt.title('MULTIMODEL SIMULATION RESULT', fontsize=48)
        plt.legend()

        # Set the locator for x and y axis
        plt.gca().xaxis.set_major_locator(ticker.MaxNLocator(5))
        plt.gca().yaxis.set_major_locator(ticker.MaxNLocator(3))

        # Set the size of the ticks
        plt.tick_params(axis='both', which='major', labelsize=28)

        plt.savefig(f'{metric_name}.png', transparent=False)  # Save the figure with a transparent background
        plt.show()

    # todo here a plotter for the metamodel!
    def _plot_meta_model(self, metric_name, y_label):
        median_values = []
        dataframe = self.dfs[0]

        # Step 1 and 2: Resample to 1-second granularity and calculate median
        for i in self.dfs:
            pass


        # Step 3: Concatenate all median series to create a new dataframe
        median_df = pd.concat(median_values, axis=1)

        # Step 4: Calculate the median across the columns for each row
        metamodel = median_df.median(axis=1)

        # Step 5: Plot the metamodel
        plt.figure(figsize=(12, 10))
        plt.plot(metamodel, label='Metamodel', color='purple')
        plt.xlabel('Time (s)', fontsize=16)
        plt.ylabel(y_label, fontsize=16)
        plt.title(f'{y_label} for Metamodel', fontsize=16)
        plt.legend()
        plt.show()


    # this method works like _plot_metric, but is generated a plot for each model, in individual figures
    def _plot_metric_individually(self, metric_name, y_label):
        model_names = ['SQRT', 'LINEAR', 'SQUARE', 'CUBIC']
        colors = ['#0E2A7A', '#F5D34B', '#BD2D2F', '#0E2A7A']
        for i, df in enumerate(self.dfs):
            plt.figure(figsize=(12, 10))
            plt.plot(df[metric_name][:40], label=f'Model {str.lower(model_names[i])}', color=colors[i], linewidth=5)

            # Remove the ticks from x and y labels
            plt.tick_params(axis='x', which='both', bottom=False, top=False, labelbottom=False)
            plt.tick_params(axis='y', which='both', left=False, right=False, labelleft=False)
            plt.title("MODEL " + model_names[i], fontsize=32)

            plt.savefig(f'individual_models/{model_names[i]}_{metric_name}.png',
                        transparent=False)  # Save the figure with a transparent background
            plt.show()

    def _plot_the_plot(self, metric_values, y_label):
        plt.figure(figsize=(12, 10))
        for i, metric_value in enumerate(metric_values):
            plt.plot(metric_value, label=f'Model {i + 1}', color=self.colors[i % len(self.colors)])
        plt.xlabel('Time (s)', fontsize=16)
        plt.ylabel(y_label, fontsize=16)
        plt.title(f'{y_label} for each model', fontsize=16)
        plt.legend()
        plt.show()
