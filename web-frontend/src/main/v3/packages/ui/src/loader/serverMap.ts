import {
  Configuration,
  SEARCH_PARAMETER_DATE_FORMAT,
  SEARCH_PARAMETER_DATE_FORMAT_WHITE_LIST,
} from '@pinpoint-fe/ui/src/constants';
import { getConfiguration } from '@pinpoint-fe/ui/src/hooks';
import {
  convertParamsToQueryString,
  getApplicationTypeAndName,
  getFormattedDateRange,
  getParsedDateRange,
  getTimezone,
  isValidDateRange,
} from '@pinpoint-fe/ui/src/utils';
import { parse } from 'date-fns';
import { formatInTimeZone } from 'date-fns-tz';
import { LoaderFunctionArgs, redirect } from 'react-router-dom';

export const serverMapRouteLoader = async ({ params, request }: LoaderFunctionArgs) => {
  try {
    const application = getApplicationTypeAndName(params.application!);
    const configuration = await getConfiguration<Configuration>();
    const timezone = getTimezone();

    if (application?.applicationName && application.serviceType) {
      const basePath = `/serverMap/${params.application}`;
      const queryParam = Object.fromEntries(new URL(request.url).searchParams);
      const conditions = Object.keys(queryParam);

      const from = queryParam?.from as string;
      const to = queryParam?.to as string;

      const currentDate = new Date();
      const validationRange = isValidDateRange(configuration?.['periodMax.serverMap'] || 2);
      const defaultParsedDateRange = getParsedDateRange({ from, to });
      const defaultFormattedDateRange = {
        from: formatInTimeZone(defaultParsedDateRange.from, timezone, SEARCH_PARAMETER_DATE_FORMAT),
        to: formatInTimeZone(defaultParsedDateRange.to, timezone, SEARCH_PARAMETER_DATE_FORMAT),
      };
      const defaultDatesQueryString = new URLSearchParams(defaultFormattedDateRange).toString();
      const defaultDestination = `${basePath}?${defaultDatesQueryString}`;

      if (conditions.length === 0) {
        return redirect(defaultDestination);
      } else if (conditions.includes('from')) {
        if (!conditions.includes('to')) {
          return redirect(defaultDestination);
        }

        const matchedFormat = SEARCH_PARAMETER_DATE_FORMAT_WHITE_LIST.find((dateFormat) => {
          const parsedDateRange = {
            from: parse(from, dateFormat, currentDate),
            to: parse(to, dateFormat, currentDate),
          };

          return validationRange(parsedDateRange);
        });

        if (!matchedFormat) {
          return redirect(defaultDestination);
        }

        if (matchedFormat !== SEARCH_PARAMETER_DATE_FORMAT) {
          const parsedDateRange = {
            from: parse(from, matchedFormat, currentDate),
            to: parse(to, matchedFormat, currentDate),
          };
          const formattedDataRange = getFormattedDateRange(parsedDateRange);
          const destination = `${basePath}?${convertParamsToQueryString({
            ...queryParam,
            ...formattedDataRange,
          })}`;
          return redirect(destination);
        }

        return application;
      }
    }

    return application;
  } catch (error) {
    console.error('Error in serverMapRouteLoader:', error);
    return null;
  }
};
