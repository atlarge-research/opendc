#  Copyright (c) 2021 AtLarge Research
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in all
#  copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
#  SOFTWARE.

import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.ticker as mtick
import seaborn as sns

def figsize(height=None):
    width = (448.1309 / 72.27) # LaTeX linewidth divided by DPI
    if not height:
        height = width / ((1 + 5 ** 0.5) / 2)
    return width, height


def set(font_scale=0.8):
    """
    Configure the Matplotlib and Seaborn styling for the figures.
    """
    theme = {
        'legend.fancybox': False,
        'legend.edgecolor': 'black',
        'xtick.major.pad': 0,
        'ytick.major.pad': 0,
    }

    rc = {
        'pdf.fonttype': 42,
        'figure.figsize': figsize(3),  # Derived from thesis dimensions
        'figure.autolayout': False,
        'figure.constrained_layout.use': True,
        'figure.dpi': 300,
    }

    sns.set_theme(style="darkgrid", rc=theme | rc, font_scale=font_scale)


def align_labels(ax, va=None, ha=None, ma=None, offset=0.8, axis='x'):
    if axis == 'x':
        x = ax.xaxis
    else:
        x = ax.yaxis

    for lab in x.get_ticklabels():
        if ha:
            lab.set_horizontalalignment(ha)
        if va:
            lab.set_verticalalignment(va)
        if ma:
            lab.set_multialignment(ma)
    fontsize = x.get_ticklabels()[0].get_size()
    ax.tick_params(axis=axis, pad=fontsize+offset)


def plot_risk_factors(data, height=3):
    """
    Plot the risk factors using a boxplot.
    :param data: The data to plot
    :return: A tuple of the figure and axes.
    """
    fig, ax = plt.subplots(3, 1, figsize=figsize(height), sharex=True, gridspec_kw={'height_ratios': [3, 4, 1]})

    cx_data = data[data['id'].str.startswith('customer')]
    cx_order = ['customer:availability', 'customer:cpu_interference', 'customer:latency']
    cx_labels = ['Availability', 'CPU Interference', 'Scheduling Latency']

    co_data = data[data['id'].str.startswith('company')]
    co_order = ['company:power', 'company:co2', 'company:host_saturation', 'company:host_imbalance']
    co_labels = ['Electricity', 'CO2 Emissions', 'Host Saturation', 'Host Imbalance']

    soc_data = data[data['id'].str.startswith('society')]
    soc_order = ['society:co2']
    soc_labels = ['CO2 Emissions']

    sns.boxplot(x="cost", y="id", data=cx_data, ax=ax[0], showfliers=False, showmeans=True, order=cx_order)

    ax[0].set_xlabel("")
    ax[0].set_yticklabels(cx_labels)
    ax[0].set_ylabel("Customer")

    sns.boxplot(x="cost", y="id", data=co_data, ax=ax[1], showfliers=False, showmeans=True, order=co_order)

    ax[1].set_xlabel("")
    ax[1].set_yticklabels(co_labels)
    ax[1].set_ylabel("Company")

    sns.boxplot(x="cost", y="id", data=soc_data, ax=ax[2], showfliers=False, showmeans=True, order=soc_order)

    ax[2].set_xlabel("Risk per Month (€)", usetex=False)
    ax[2].set_yticklabels(soc_labels)
    ax[2].set_ylabel("Society")

    fig.align_ylabels(ax)

    return fig, ax


def plot_risk_factors_horizontal(data, hue=None, hue_order=None, height=2.1, bar=False):
    fig, ax = plt.subplots(1, 3, figsize=figsize(height), sharey=True, gridspec_kw={'width_ratios': [3, 4, 1]})

    cx_data = data[data['id'].str.startswith('customer')]
    cx_order = ['customer:availability', 'customer:latency', 'customer:cpu_interference']
    cx_labels = ['Availability', 'Scalability', 'QoS']

    co_data = data[data['id'].str.startswith('company')]
    co_order = ['company:power', 'company:co2', 'company:host_saturation', 'company:host_imbalance']
    co_labels = ['Electricity\nDemand', 'CO2\nEmissions', 'Resource\nSaturation', 'Resource\nImbalance']

    soc_data = data[data['id'].str.startswith('society')]
    soc_order = ['society:co2']
    soc_labels = ['CO2\nEmissions']

    color = 'C0' if not hue else None
    fmt = mtick.StrMethodFormatter('{x:,.0f}')

    if bar:
        sns.barplot(x="id", y="cost", data=cx_data, ax=ax[0], order=cx_order, hue=hue, hue_order=hue_order, edgecolor='black', color=color)
    else:
        sns.boxplot(x="id", y="cost", data=cx_data, ax=ax[0], order=cx_order, hue=hue, hue_order=hue_order, showfliers=False, showmeans=True, width=0.5, linewidth=0.9, color=color)

    ax[0].set_ylabel("Risk per Month (€)")
    ax[0].set_xticklabels(cx_labels)
    ax[0].set_xlabel("")
    ax[0].set_title("Customer")
    ax[0].yaxis.set_major_formatter(fmt)

    if bar:
        sns.barplot(x="id", y="cost", data=co_data, ax=ax[1], order=co_order, hue=hue, hue_order=hue_order, edgecolor='black', color=color)
    else:
        sns.boxplot(x="id", y="cost", data=co_data, ax=ax[1], order=co_order, hue=hue, hue_order=hue_order, showfliers=False, showmeans=True, width=0.5, linewidth=0.9, color=color)

    ax[1].set_ylabel("")
    ax[1].set_xticklabels(co_labels)
    ax[1].set_xlabel("")
    ax[1].set_title("Company")
    ax[1].legend([],[], frameon=False)
    ax[1].yaxis.set_major_formatter(fmt)


    if bar:
        sns.barplot(x="id", y="cost", data=soc_data, ax=ax[2], order=soc_order, hue=hue, hue_order=hue_order, edgecolor='black', color=color)
    else:
        sns.boxplot(x="id", y="cost", data=soc_data, ax=ax[2], order=soc_order, hue=hue, hue_order=hue_order, showfliers=False, showmeans=True, width=0.5, linewidth=0.9, color=color)

    ax[2].set_ylabel("")
    ax[2].set_xticklabels(soc_labels)
    ax[2].set_xlabel("")
    ax[2].set_title("Society")
    ax[2].yaxis.set_major_formatter(fmt)
    ax[2].legend([],[], frameon=False)

    fig.align_xlabels(ax)

    for x in ax:
        align_labels(x, va='center')

    return fig, ax

def compute_periodic_risk(risk, freq='M', keys=['id']):
    """
    Compute the periodic risk for the specified keys.

    :param risk: The raw data to compute the monthly risk from.
    :param freq: The period to compute the risk for.
    :param keys: The grouping keys.
    :return: The risk grouped by the period, keys and seed.
    """
    return risk.groupby(keys + ['seed', pd.Grouper(key='timestamp', freq=freq)], sort=False)['cost'].sum().reset_index()


def compute_monthly_risk(risk, keys=['id'], observed=False):
    """
    Compute the monthly risk for the specified keys.

    :param risk: The raw data to compute the monthly risk from.
    :param keys: The grouping keys.
    :return: The risk grouped by the period, keys and seed.
    """
    group = keys + ['seed']
    as_index = True

    if 'month' in risk.columns:
        group.append('month')
        as_index = False
    else:
        group.append(risk['timestamp'].dt.month.rename('month'))

    res = risk.groupby(group, sort=False, as_index=as_index, observed=observed)['cost'].sum()

    if as_index:
        return res.reset_index()
    return res


def adjust_env(risk, energy_price=300, co2_price=64, social_co2_price=360.11, pue=1.57):
    """
    Adjust the prices of the environmental risk.
    :param risk: The raw data to adjust.
    :param energy_price: The price of energy per MWh
    :param co2_price: The price per tCO2
    :param social_co2_price: The social price per tCO2
    :param pue: The PUE of the datacenter.
    :return: The adjusted risk values.
    """
    id = risk['id']
    cost = risk['cost'] \
        .where(id != 'company:power', risk['value'] * (energy_price / 1000) * (pue / 1.57)) \
        .where(id != 'company:co2', risk['value'] * (co2_price / 1000) * (pue / 1.57)) \
        .where(id != 'society:co2', risk['value'] * (social_co2_price / 1000) * (pue / 1.57))
    return risk.assign(cost=cost)


def adjust_pue(risk, pue=1.57, co2_factor=0.556):
    """
    Adjust the PUE of the datacenter.
    :param risk: The raw risk data.
    :param pue: The new PUE of the datacenter.
    :param co2_factor: The new CO2 factor to use.
    :return: The adjusted risk values.
    """
    id = risk['id']
    cost = risk['cost'] \
        .where(id != 'company:power', risk['cost'] * (pue / 1.57)) \
        .where(id != 'company:co2', risk['cost'] * (pue / 1.57) * (co2_factor / 0.556)) \
        .where(id != 'society:co2', risk['cost'] * (pue / 1.57) * (co2_factor / 0.556))
    return risk.assign(cost=cost)


def adjust_co2(risk, co2_price=64, social_co2_price=360.11, pue=1.57):
    """
    Adjust the CO2 prices.
    :param risk: The raw risk data.
    :param co2_price: The price per tCO2.
    :param social_co2_price: The social price per tCO2.
    :param pue: The adjusted PUE of the datacenter.
    :return: The adjusted risk values.
    """
    id = risk['id']
    cost = risk['cost'] \
        .where(id != 'company:power', risk['cost'] * (pue / 1.57)) \
        .where(id != 'company:co2', risk['cost'] * (co2_price / 64) * (pue / 1.57)) \
        .where(id != 'society:co2', risk['cost'] * (social_co2_price / 360.11) * (pue / 1.57))
    return risk.assign(cost=cost)


def adjust_penalty(risk, penalties, vcpu_cost = 0.040):
    """
    Adjust the SLA penalties.
    :param risk: The raw risk data to adjust.
    :param penalties: The penalties to use.
    :return: The adjusted risk values.
    """
    av = risk['id'] == 'customer:availability'

    def m(av):
        for (bound, p) in penalties:
            if av < bound:
                return p
        return 0.0

    return risk.assign(cost=risk['cost'].where(~av, other=risk['cost'] * risk.loc[av, 'value'].apply(m)))
